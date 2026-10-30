
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME680.h>
#include <Adafruit_CCS811.h>
#include <WiFi.h>
#include <WebServer.h>

// ==============================
// PIN CONFIGURATION
// ==============================

// BME680
#define BME_SDA 21
#define BME_SCL 22

// CCS811
#define CCS_SDA 16
#define CCS_SCL 17

// MQ-3
#define MQ3_PIN 34

// ==============================
// I2C BUSES
// ==============================

TwoWire I2C_BME = TwoWire(0);
TwoWire I2C_CCS = TwoWire(1);

// ==============================
// SENSOR OBJECTS
// ==============================

Adafruit_BME680 bme(&I2C_BME);
Adafruit_CCS811 ccs;

bool bmeConnected = false;
bool ccsConnected = false;

// ==============================
// WIFI CONFIGURATION
// ==============================

//  NEED TO CHANGE THIS LATER ON
// PROBLEM IS THAT ITS NOT REALLY USEFUL 
const char* ssid = "iPhone";
const char* password = "23456788";

WebServer server(80);

// ==============================
// SENSOR DATA
// ==============================

// all these values are retrived and stored inside the application

float temperature = 0;
float humidity = 0;
float pressure = 0;
float gasResistance = 0;

int eco2 = 0;
int tvoc = 0;
int mq3Raw = 0;

bool bmeDataValid = false;
bool ccsDataValid = false;

// ==============================
// HTTP SERVER
// ==============================


// Function to format the JSON that will be sent
void handleSensors() {

  String json = "{";

  // BME680 data
  if (bmeDataValid)
  {
    json += "\"temperature\":" + String(temperature, 2);
    json += ",\"humidity\":" + String(humidity, 2);
    json += ",\"pressure\":" + String(pressure, 2);
    json += ",\"gasResistance\":" + String(gasResistance, 2);
  }
  else
  {
    json += "\"temperature\":null";
    json += ",\"humidity\":null";
    json += ",\"pressure\":null";
    json += ",\"gasResistance\":null";
  }

  // CCS811 data
  if (ccsDataValid)
  {
    json += ",\"eco2\":" + String(eco2);
    json += ",\"tvoc\":" + String(tvoc);
  } 
  else 
  {
    json += ",\"eco2\":null";
    json += ",\"tvoc\":null";
  }

  // MQ-3 data
  json += ",\"mq3Raw\":" + String(mq3Raw);

  json += "}";

  // IMPORTANT TO NOTE THAT RIGHT NOW WE ARE ONLY SENDING A SINGLE REQUEST 
  // UPDATE THIS IN THE FUTURE TO SEND MULTIPLE REQUEST BECAUSE SENSOR NEED TO UPLOAD DATA CONTINUOUSLY 

  server.send(200, "application/json", json);
}

// ==============================
// WIFI CONNECTION
// ==============================

void connectToWiFi() 
{

  Serial.println("\nConnecting to the hotspot...");

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  unsigned long startTime = millis();

  // Wait up to 20 seconds
  while (WiFi.status() != WL_CONNECTED &&
         millis() - startTime < 20000) 
  {
    delay(500);
    Serial.print(".");
  }

  Serial.println();

  if (WiFi.status() == WL_CONNECTED) 
  {

    Serial.println("Wi-Fi connected!");

    Serial.print("ESP32 IP: ");
    Serial.println(WiFi.localIP()); // prints the ip

  } 
  else 
  {
    Serial.println("ERROR: Wi-Fi connection failed!");
    Serial.print("Wi-Fi status: ");
    Serial.println(WiFi.status());
  }
}

// ==============================
// SETUP
// ==============================

void setup() 
{

  Serial.begin(115200);
  delay(1500);

  Serial.println();
  Serial.println("==============================");
  Serial.println("    THREE SENSOR TEST");
  Serial.println("==============================");

  // ------------------------------
  // INITIALIZE I2C
  // ------------------------------

  I2C_BME.begin(BME_SDA, BME_SCL, 100000);
  I2C_CCS.begin(CCS_SDA, CCS_SCL, 100000);

  // ------------------------------
  // INITIALIZE MQ-3
  // ------------------------------

  analogReadResolution(12);
  analogSetPinAttenuation(MQ3_PIN, ADC_11db);

  // ------------------------------
  // INITIALIZE BME680
  // ------------------------------

  Serial.println("\nInitializing BME680...");

  if (bme.begin(0x76) || bme.begin(0x77)) 
  {

    bmeConnected = true;
    Serial.println("BME680 connected!");

    bme.setTemperatureOversampling(BME680_OS_8X);
    bme.setHumidityOversampling(BME680_OS_2X);
    bme.setPressureOversampling(BME680_OS_4X);
    bme.setIIRFilterSize(BME680_FILTER_SIZE_3);
    bme.setGasHeater(320, 150);

  } 
  else 
  {
    Serial.println("ERROR: BME680 not found!");
  }

  // ------------------------------
  // INITIALIZE CCS811
  // ------------------------------

  Serial.println("\nInitializing CCS811...");

  if (ccs.begin(0x5A, &I2C_CCS) ||
      ccs.begin(0x5B, &I2C_CCS)) 
  {

    ccsConnected = true;
    Serial.println("CCS811 connected!");

  } 
  else 
  {
    Serial.println("ERROR: CCS811 not found!");
  }

  // ------------------------------
  // INITIALIZE MQ-3
  // ------------------------------

  Serial.println("\nInitializing MQ-3...");
  Serial.println("MQ-3 analog input ready.");
  Serial.println("Allow sensor to warm up.");

  
  
  connectToWiFi();

  // ------------------------------
  // START HTTP SERVER
  // ------------------------------

  server.on("/sensors", HTTP_GET, handleSensors);
  server.begin();

  Serial.println("HTTP server started!");
  Serial.println("\nSetup complete!");
  // Was used to send the data between ESP and Application 
}

// ==============================
// MAIN LOOP
// ==============================

void loop() 
{

  // Handle incoming HTTP requests
  server.handleClient();

  // Retry Wi-Fi connection if disconnected
  static unsigned long lastWiFiRetry = 0;

  if (WiFi.status() != WL_CONNECTED &&
      millis() - lastWiFiRetry >= 10000) 
  {

    lastWiFiRetry = millis();
    Serial.println("Wi-Fi disconnected. Reconnecting...");
    WiFi.reconnect();
  }

  // Read sensors every 2 seconds
  static unsigned long lastReading = 0;

  if (millis() - lastReading < 2000) 
  {
    delay(1);
    return;
  }

  lastReading = millis();

  Serial.println();
  Serial.println("==============================");
  Serial.println("       SENSOR READINGS");
  Serial.println("==============================");

  // ------------------------------
  // BME680
  // ------------------------------

  Serial.println("\n--- BME680 ---");

  if (bmeConnected) 
  {

    if (bme.performReading()) 
    {

      temperature = bme.temperature;
      humidity = bme.humidity;
      pressure = bme.pressure / 100.0;
      gasResistance = bme.gas_resistance / 1000.0;

      bmeDataValid = true;

      Serial.print("Temperature: ");
      Serial.print(temperature);
      Serial.println(" C");

      Serial.print("Humidity: ");
      Serial.print(humidity);
      Serial.println(" %");

      Serial.print("Pressure: ");
      Serial.print(pressure);
      Serial.println(" hPa");

      Serial.print("Gas Resistance: ");
      Serial.print(gasResistance);
      Serial.println(" kOhms");

    }
    else 
    {
      bmeDataValid = false;
      Serial.println("ERROR: Reading failed!");
    }

  } 
  else 
  {
    Serial.println("Sensor not connected.");
  }

  // ------------------------------
  // CCS811
  // ------------------------------

  Serial.println("\n--- CCS811 ---");

  if (ccsConnected) 
  {

    if (ccs.available()) 
    {

      if (!ccs.readData()) 
      {

        eco2 = ccs.geteCO2();
        tvoc = ccs.getTVOC();

        ccsDataValid = true;

        Serial.print("eCO2: ");
        Serial.print(eco2);
        Serial.println(" ppm");

        Serial.print("TVOC: ");
        Serial.print(tvoc);
        Serial.println(" ppb");

      } 
      else 
      {
        ccsDataValid = false;
        Serial.println("ERROR: Reading failed!");
      }

    } 
    else 
    {
      Serial.println("Waiting for new data...");
    }

  } 
  else 
  {
    Serial.println("Sensor not connected.");
  }

  // ------------------------------
  // MQ-3
  // ------------------------------

  Serial.println("\n--- MQ-3 ALCOHOL SENSOR ---");

  // Average 10 readings to reduce noise
  long total = 0;

  for (int i = 0; i < 10; i++) 
  {
    total += analogRead(MQ3_PIN);
    delay(10);
  }

  mq3Raw = total / 10;

  // ESP32 ADC voltage after voltage divider
  float adcVoltage =
      analogReadMilliVolts(MQ3_PIN) / 1000.0;

  // Assumes 20k / 20k voltage divider
  float sensorVoltage = adcVoltage * 2.0;

  Serial.print("Analog Value: ");
  Serial.print(mq3Raw);
  Serial.println(" / 4095");

  Serial.print("ADC Voltage: ");
  Serial.print(adcVoltage, 3);
  Serial.println(" V");

  Serial.print("Sensor Output Voltage: ");
  Serial.print(sensorVoltage, 3);
  Serial.println(" V");

  Serial.println("\n==============================");
}

