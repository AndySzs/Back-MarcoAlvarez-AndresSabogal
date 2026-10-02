# BluePrints en Tiempo Real — Backend (ARSW Lab P4)

Backend del laboratorio **BluePrints en Tiempo Real**: en un mismo servidor Spring Boot (puerto `8080`) expone
una **API REST** CRUD de planos y **colaboración en tiempo real con STOMP** sobre WebSocket.
El frontend (React + Vite, `http://localhost:5173`) vive en otro repositorio.

## Integrantes

- Marco Alvarez
- Andres Sabogal

## Stack

Java 21 · Spring Boot 3.3 · Spring Web · Spring WebSocket (STOMP, simple broker) · Bean Validation ·
Actuator · springdoc-openapi (Swagger UI).

## Cómo correr

Requisitos: JDK 21 y Maven 3.9+ (este repo **no** trae `mvnw`).

```bash
mvn spring-boot:run          # levanta en http://localhost:8080
mvn clean test               # unit + MockMvc + integración STOMP
```

Orígenes permitidos (CORS y handshake WebSocket) configurables con la variable `ALLOWED_ORIGINS`
(por defecto `http://localhost:5173,http://127.0.0.1:5173`):

```bash
ALLOWED_ORIGINS=http://localhost:5173,https://mi-front.example.com mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health → `{"status":"UP"}`
- Ejemplos de todas las peticiones en [`api.http`](api.http).

### Datos semilla (canvas 600x400)

| author | name    | puntos |
|--------|---------|--------|
| juan   | plano-1 | 4      |
| juan   | plano-2 | 3      |
| john   | house   | 6      |
| jane   | garden  | 5      |

## API REST

Base: `/api/v1/blueprints` (alias equivalente: `/api/blueprints`).
Todas las respuestas van envueltas en `{ "code": <int>, "message": <string>, "data": <T> }`.

| Método | Ruta                          | Body                         | Éxito                      | Errores             | Evento STOMP |
|--------|-------------------------------|------------------------------|----------------------------|---------------------|--------------|
| GET    | `/`                           | —                            | 200, `Blueprint[]`         | —                   | —            |
| GET    | `/?author={author}`           | —                            | 200, `AuthorBlueprints`    | —                   | —            |
| GET    | `/{author}`                   | —                            | 200, `AuthorBlueprints`    | —                   | —            |
| GET    | `/{author}/{name}`            | —                            | 200, `Blueprint`           | 404                 | —            |
| POST   | `/`                           | `{author, name, points[]}`   | 201, `Blueprint`           | 400, 409            | —            |
| PUT    | `/{author}/{name}`            | `{points[]}`                 | 200, `Blueprint`           | 400, 404            | `replace`    |
| PUT    | `/{author}/{name}/points`     | `{x, y}`                     | 202, `Blueprint`           | 400, 404            | `points`     |
| DELETE | `/{author}/{name}`            | —                            | 200, `data: null`          | 404                 | `deleted`    |

- `Blueprint` = `{ "author": "juan", "name": "plano-1", "points": [ {"x":50,"y":50}, ... ] }`
- `AuthorBlueprints` = `{ "author": "juan", "totalPoints": 7, "blueprints": [Blueprint, ...] }`
  (si el autor no tiene planos: 200 con `blueprints: []` y `totalPoints: 0`).
- Validación: `author` y `name` obligatorios, máx. 50 caracteres, regex `[A-Za-z0-9_-]+`; coordenadas en `[-10000, 10000]`.
- Errores: 400 de validación trae en `data` un mapa campo → mensaje (p.ej. `{"author": "must match ...", "points[0].x": "must be less than or equal to 10000"}`);
  JSON mal formado = 400; plano inexistente = 404; plano duplicado = 409; ruta inexistente = 404.

## Protocolo STOMP

| Elemento            | Valor                                              |
|---------------------|----------------------------------------------------|
| Endpoint WebSocket  | `ws://localhost:8080/ws-blueprints` (STOMP nativo, sin SockJS) |
| Enviar un punto     | `SEND /app/draw`                                   |
| Suscribirse a plano | `SUBSCRIBE /topic/blueprints.{author}.{name}`      |

**Entrada** (cliente → `/app/draw`):

```json
{ "author": "juan", "name": "plano-1", "point": { "x": 120, "y": 80 }, "senderId": "tab-3f2a", "ts": 1727800000000 }
```

`senderId` y `ts` son opcionales; el servidor los devuelve tal cual para que el front reconozca su propio eco
y mida latencia (`Date.now() - ts`). Si el evento es inválido (author/name fuera del regex, `point` nulo o
coordenadas fuera de rango) se descarta y se registra un `WARN`.

**Salida** (servidor → `/topic/blueprints.{author}.{name}`):

```json
{ "type": "points", "author": "juan", "name": "plano-1", "points": [ { "x": 120, "y": 80 } ], "senderId": "tab-3f2a", "ts": 1727800000000 }
```

| `type`    | Origen                                   | `points`                          | Qué hace el front           |
|-----------|------------------------------------------|-----------------------------------|-----------------------------|
| `points`  | `/app/draw` o `PUT .../points`           | Puntos nuevos (normalmente 1)     | Los agrega al final         |
| `replace` | `PUT /{author}/{name}`                   | **Todos** los puntos del plano    | Reemplaza su lista completa |
| `deleted` | `DELETE /{author}/{name}`                | `[]`                              | Limpia / cierra el plano    |

En eventos originados por REST, `senderId` es `null` y `ts` es la hora del servidor.

## Decisiones de diseño

- **Un tópico por plano** (`/topic/blueprints.{author}.{name}`): aislamiento — quien edita `juan/plano-1` no recibe
  tráfico de otros planos, y el broker hace el filtrado. El regex de `author`/`name` impide `.` y `/`, así que el
  nombre del tópico no es ambiguo.
- **Persistir cada punto en el servidor**: `/app/draw` guarda el punto antes de difundirlo, de modo que una pestaña
  que abre el plano después obtiene el estado actual con un `GET`. Si el plano aún no existe, el punto se reenvía
  pero no se persiste.
- **Eco al emisor**: el simple broker entrega el mensaje a *todos* los suscriptores, incluido quien lo envió.
  Por eso el front **no debe pintar dos veces**: o pinta solo al recibir el eco, o pinta optimista y descarta los
  mensajes cuyo `senderId` sea el suyo.
- **Thread-safety**: los mensajes STOMP se procesan en un pool de hilos, así que varios clientes pueden agregar
  puntos al mismo plano a la vez. `Blueprint` sincroniza `addPoint`, `replacePoints`, `getPoints` (devuelve copia
  inmutable) y `getPointsCount`; la persistencia usa `ConcurrentHashMap` con `putIfAbsent` para crear sin carreras.
  Hay un test con 10 hilos × 100 puntos que verifica que no se pierde ninguno.
- **Sincronización REST ↔ tiempo real**: los cambios hechos por REST (`PUT`, `PUT /points`, `DELETE`) también se
  publican en el tópico, así que todas las pestañas quedan sincronizadas sin importar el canal usado.
- **Observabilidad**: `WebSocketEventsLogger` registra conexiones, desconexiones, suscripciones y el número de
  sesiones activas; `/actuator/health` sirve como health check.
- **Sin Spring Security**: JWT es opcional en este laboratorio y el front no tiene login.

## Comparativa: Socket.IO vs STOMP

| Aspecto           | Socket.IO                                                     | STOMP (Spring)                                                        |
|-------------------|---------------------------------------------------------------|-----------------------------------------------------------------------|
| Agrupación        | *Rooms*: el servidor hace `socket.join(room)` y emite a la room | *Topics*: el cliente se suscribe a un destino; el broker enruta       |
| Eco al emisor     | `socket.to(room).emit` excluye al emisor; `io.to(room)` lo incluye | El broker entrega a todos los suscriptores, **incluido** el emisor   |
| Ecosistema        | Node.js; protocolo propio, requiere cliente Socket.IO          | Spring / JVM; protocolo estándar de texto, broker reemplazable (RabbitMQ, ActiveMQ) |
| Reconexión        | Automática en el cliente, con buffer de eventos                | `@stomp/stompjs` reconecta con `reconnectDelay`, pero hay que re-suscribirse (lo hace en `onConnect`) |
| Mensajes          | Eventos con nombre + payload arbitrario                        | Frames con destino, headers y body (JSON)                             |

## Video

Link del video de demostración: _pendiente_

## Contrato para el front

1. **REST** en `http://localhost:8080/api/v1/blueprints`; leer siempre `response.data` (envoltorio `{code, message, data}`).
   - Cargar plano: `GET /{author}/{name}` → `data.points`.
   - Listar autor: `GET /{author}` → `data.blueprints`, `data.totalPoints`.
   - Crear: `POST /` con `{author, name, points}` (201 / 409 si existe).
   - Guardar (reemplazar): `PUT /{author}/{name}` con `{points}`.
   - Eliminar: `DELETE /{author}/{name}`.
2. **STOMP**: conectar a `ws://localhost:8080/ws-blueprints` (ej. `@stomp/stompjs` con `brokerURL`, sin SockJS).
   - Al abrir un plano: `subscribe('/topic/blueprints.' + author + '.' + name)`; al cambiar de plano, `unsubscribe` del anterior.
   - Al dibujar: `publish({ destination: '/app/draw', body: JSON.stringify({ author, name, point: {x, y}, senderId, ts: Date.now() }) })`.
   - Al recibir: según `type` → `points` agrega, `replace` reemplaza, `deleted` limpia.
   - **No pintar dos veces**: el emisor recibe su propio eco (usar `senderId` para distinguirlo).
3. Orígenes permitidos por defecto: `http://localhost:5173` y `http://127.0.0.1:5173`.
