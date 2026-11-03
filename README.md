# AppSalaAmbiental

App Android de monitoreo y control ambiental para aulas de clase. Se comunica por WiFi con un Arduino UNO R4 WiFi para medir humedad y temperatura, y controlar un ventilador en modo automático o manual.

## Tecnología

- Kotlin, interfaz en XML
- HTTP nativo (`HttpURLConnection`) + `org.json` para la comunicación con el Arduino
- Room (SQLite) para el historial local de incidentes

## Pantallas

1. **Login** — acceso con usuario y contraseña fijos (prototipo de validación, sin base de datos de usuarios).
2. **Monitoreo principal** — humedad, temperatura, estado del ambiente (Normal/Advertencia/Alto), estado del ventilador y accesos a Manual, Automático, Configuración e Historial.
3. **Control manual** — enciende/apaga el ventilador y ajusta su velocidad (0-100%).
4. **Historial** — lista de incidentes (momentos en que el ambiente salió de Normal), con inicio, fin y gravedad.

## Conectividad con el Arduino

La app consulta `GET /status` cada 2 segundos y envía órdenes por `POST /fan` y `POST /auto`. El contrato completo (JSON y endpoints) está en el sketch `AppSalaAmbiental_Arduino.ino` incluido en este repositorio.

Incluye un **modo simulación** (configurable desde la app) que permite probar toda la interfaz sin tener el Arduino físico conectado. Los valores de humedad y temperatura son generados por software, tanto en el modo simulación de la app como en el sketch del Arduino, ya que no se cuenta con un sensor DHT físico. El resto de la comunicación (WiFi, HTTP, JSON, control del ventilador) es real.

## Configuración antes de ejecutar

1. Abrir el proyecto en Android Studio.
2. En la pantalla de Configuración de la app, ingresar la IP del Arduino en la red WiFi local (router o hotspot del celular).
3. Desactivar "Modo simulación" para usar el Arduino real.

## Arduino

El sketch `AppSalaAmbiental_Arduino.ino` corre en el Arduino UNO R4 WiFi. Antes de cargarlo, completar `WIFI_SSID` y `WIFI_PASS` con los datos de la red.

## Limitaciones conocidas

- Login con credenciales fijas en el código, sin cifrado (prototipo de validación).
- Tráfico HTTP sin cifrar entre la app y el Arduino.
- La IP del Arduino debe actualizarse manualmente si cambia.
- Monitoreo por consulta periódica (polling) cada 2 segundos, no por notificaciones push.
- No se cuenta con un sensor DHT físico: la humedad y temperatura se generan por software.

## Informe

El informe técnico con la justificación de las decisiones de diseño (login, base de datos, etc.) está en `AppSalaAmbiental_Informe.docx`.
