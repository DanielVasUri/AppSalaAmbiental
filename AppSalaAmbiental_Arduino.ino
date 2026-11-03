// AppBackEnd - Servidor HTTP para Arduino UNO R4 WiFi
// Contrato: GET /status, POST /fan, POST /auto (JSON, puerto 80)
// Humedad y temperatura SIMULADAS. El LED integrado hace de "ventilador".

#include <WiFiS3.h>

// ====== RELLENA ESTOS DOS DATOS (hotspot 2.4 GHz, WPA2) ======
const char* WIFI_SSID = "TU_RED";
const char* WIFI_PASS = "TU_CLAVE";
// =============================================================

WiFiServer server(80);

bool ventiladorOn = false;
int velocidad = 0;
bool modoAuto = true;

float humedad = 55.0;
float temperatura = 24.0;
int dirHum = 1;
unsigned long ultimoCambio = 0;
unsigned long ultimoAviso = 0;
String estadoPrevio = "NORMAL";

String estadoActual() {
  int h = (int)humedad;
  if (h >= 75) return "ALTO";
  if (h >= 60) return "ADVERTENCIA";
  return "NORMAL";
}

void aplicarLogica() {
  String e = estadoActual();
  // En manual, vuelve a AUTO solo cuando el estado regresa a NORMAL
  if (!modoAuto && estadoPrevio != "NORMAL" && e == "NORMAL") {
    modoAuto = true;
  }
  estadoPrevio = e;
  if (modoAuto) {
    ventiladorOn = (e == "ALTO");
    velocidad = ventiladorOn ? 100 : 0;
  }
  digitalWrite(LED_BUILTIN, ventiladorOn ? HIGH : LOW);
}

String construirJson() {
  String j = "{";
  j += "\"hum\":" + String((int)humedad);
  j += ",\"temp\":" + String(temperatura, 1);
  j += ",\"estado\":\"" + estadoActual() + "\"";
  j += ",\"ventilador\":\"" + String(ventiladorOn ? "ON" : "OFF") + "\"";
  j += ",\"velocidad\":" + String(velocidad);
  j += ",\"modo\":\"" + String(modoAuto ? "AUTO" : "MANUAL") + "\"";
  j += "}";
  return j;
}

void responder(WiFiClient &client, int codigo, const String &cuerpo) {
  String estado = (codigo == 200) ? "OK" : (codigo == 400 ? "Bad Request" : "Not Found");
  String r = "HTTP/1.1 " + String(codigo) + " " + estado + "\r\n";
  r += "Content-Type: application/json\r\n";
  r += "Content-Length: " + String(cuerpo.length()) + "\r\n";
  r += "Connection: close\r\n\r\n";
  r += cuerpo;
  client.print(r);
  client.flush();
}

void manejarCliente(WiFiClient &client) {
  String requestLine = "";
  String linea = "";
  int contentLength = 0;
  bool primera = true;
  unsigned long inicio = millis();

  // Leer cabeceras
  while (client.connected() && millis() - inicio < 2000) {
    if (!client.available()) continue;
    char c = client.read();
    if (c == '\r') continue;
    if (c == '\n') {
      if (linea.length() == 0) break;  // fin de cabeceras
      if (primera) {
        requestLine = linea;
        primera = false;
      } else {
        String l = linea;
        l.toLowerCase();
        if (l.startsWith("content-length:")) {
          contentLength = l.substring(15).toInt();
        }
      }
      linea = "";
    } else {
      linea += c;
    }
  }
  if (contentLength > 200) contentLength = 200;

  // Leer cuerpo
  String body = "";
  while ((int)body.length() < contentLength && millis() - inicio < 2000) {
    if (client.available()) body += (char)client.read();
  }
  body.replace(" ", "");

  // Metodo y ruta
  int p1 = requestLine.indexOf(' ');
  int p2 = requestLine.indexOf(' ', p1 + 1);
  String metodo = requestLine.substring(0, p1);
  String ruta = requestLine.substring(p1 + 1, p2);
  Serial.println("Peticion: " + metodo + " " + ruta + " " + body);

  if (ruta == "/fan") {
    bool enciende = body.indexOf("\"encendido\":true") >= 0;
    Serial.println(enciende ? ">> Orden recibida: ENCENDER ventilador" : ">> Orden recibida: APAGAR ventilador");
  } else if (ruta == "/auto") {
    Serial.println(">> Orden recibida: Volver a modo AUTOMATICO");
  } else if (ruta == "/status") {
    Serial.println(">> Consulta de estado recibida");
  }

  if (metodo == "GET" && ruta == "/status") {
    responder(client, 200, construirJson());

  } else if (metodo == "POST" && ruta == "/fan") {
    bool tieneOn = body.indexOf("\"encendido\":true") >= 0;
    bool tieneOff = body.indexOf("\"encendido\":false") >= 0;
    int idx = body.indexOf("\"velocidad\":");
    if ((!tieneOn && !tieneOff) || idx < 0) {
      responder(client, 400, "{\"error\":\"cuerpo invalido\"}");
    } else {
      int v = body.substring(idx + 12).toInt();
      if (v < 0) v = 0;
      if (v > 100) v = 100;
      modoAuto = false;
      ventiladorOn = tieneOn;
      velocidad = v;
      digitalWrite(LED_BUILTIN, ventiladorOn ? HIGH : LOW);
      responder(client, 200, construirJson());
    }

  } else if (metodo == "POST" && ruta == "/auto") {
    modoAuto = true;
    aplicarLogica();
    responder(client, 200, construirJson());

  } else {
    responder(client, 404, "{\"error\":\"ruta no encontrada\"}");
  }

  delay(1);
  client.stop();
}

void conectarWiFi() {
  Serial.print("Conectando a: ");
  Serial.println(WIFI_SSID);
  while (WiFi.status() != WL_CONNECTED) {
    WiFi.begin(WIFI_SSID, WIFI_PASS);
    delay(6000);
    if (WiFi.status() != WL_CONNECTED) {
      Serial.println("Aun sin conexion, reintentando...");
    }
  }
  Serial.println("=== CONECTADO ===");
  Serial.print("IP del Arduino: ");
  Serial.println(WiFi.localIP());
}

void setup() {
  Serial.begin(115200);
  unsigned long t = millis();
  while (!Serial && millis() - t < 3000) {}
  pinMode(LED_BUILTIN, OUTPUT);

  if (WiFi.status() == WL_NO_MODULE) {
    Serial.println("No se detecta el modulo WiFi");
    while (true) {}
  }
  conectarWiFi();
  server.begin();
  Serial.println("Servidor HTTP iniciado en el puerto 80");
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("Se perdio el WiFi, reconectando...");
    conectarWiFi();
    server.begin();
  }

  // Simulacion lenta de humedad (sube y baja entre 40 y 90)
  if (millis() - ultimoCambio >= 1000) {
    ultimoCambio = millis();
    humedad += dirHum * 1.5;
    if (humedad >= 90) dirHum = -1;
    if (humedad <= 40) dirHum = 1;
    temperatura = 22.0 + (humedad - 40.0) * 0.05;
    aplicarLogica();
  }

  // Recordatorio de la IP cada 10 s
  if (millis() - ultimoAviso >= 10000) {
    ultimoAviso = millis();
    Serial.print("IP: ");
    Serial.print(WiFi.localIP());
    Serial.print(" | hum=");
    Serial.print((int)humedad);
    Serial.print(" | modo=");
    Serial.println(modoAuto ? "AUTO" : "MANUAL");
  }

  WiFiClient client = server.available();
  if (client) {
    manejarCliente(client);
  }
}
