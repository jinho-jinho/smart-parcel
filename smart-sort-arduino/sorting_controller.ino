#include <Arduino.h>
#include <Servo.h>

#define SENSOR_PIN 2         // 감지용 포토인터럽터
#define CHECK_SENSOR_PIN 3   // 검수용 포토인터럽터

#define SERVO_PIN  9         // 서보모터 신호핀

const unsigned long DEBOUNCE_MS = 30;
const unsigned long CHECK_DEBOUNCE_MS = 20;   // 검수센서 디바운스

const int HOME_ANGLE = 180;     // 서보모터 기본 각도

Servo gateServo;
int lastStable = HIGH;    // 감지센서 안정된 값
int lastCheckStable = HIGH;    // 검수센서 안정된 값

unsigned long lastChangeMs = 0;
unsigned long lastCheckChangeMs = 0; 

String rxBuf;

void setup() {
  Serial.begin(115200);
  delay(2000);
  Serial.println("READY"); // 파이썬 연결 확인용

  pinMode(SENSOR_PIN, INPUT_PULLUP);        // 감지센서
  pinMode(CHECK_SENSOR_PIN, INPUT_PULLUP);  // 검수센서

  gateServo.attach(SERVO_PIN);
  gateServo.write(HOME_ANGLE);
}

void handleCommand(const String& line) {
  int sp = line.indexOf(' ');
  String cmd = (sp == -1) ? line : line.substring(0, sp);
  cmd.trim();
  cmd.toUpperCase();

  if (cmd == "PING") {
    Serial.println("PONG");
    return;
  }

  if (cmd == "HOME") {
    gateServo.write(HOME_ANGLE);
    Serial.print("SERVO_OK ");
    Serial.println(HOME_ANGLE);
    return;
  }

  if (cmd == "SERVO") {
    String arg = (sp == -1) ? "" : line.substring(sp + 1);
    arg.trim();
    int angle = constrain(arg.toInt(), 0, 180);
    gateServo.write(angle);
    Serial.print("SERVO_OK ");
    Serial.println(angle);
    return;
  }
}

void loop() {
  unsigned long now = millis();

  // 1) 기존 포토인터럽터 PIN 2 처리
  int cur = digitalRead(SENSOR_PIN);

  if (cur != lastStable) {
    if (now - lastChangeMs >= DEBOUNCE_MS) {
      lastStable = cur;
      lastChangeMs = now;

      if (lastStable == LOW) {
        Serial.println("0");   // 기존 센서 LOW 감지
      } else {
        Serial.println("1");   // 기존 센서 HIGH 감지
      }
    }
  } else {
    lastChangeMs = now;
  }


  // 2) 검수용 포토인터럽터 PIN 3 처리
  int curCheck = digitalRead(CHECK_SENSOR_PIN);

  if (curCheck != lastCheckStable) {
    if (now - lastCheckChangeMs >= CHECK_DEBOUNCE_MS) {
      lastCheckStable = curCheck;
      lastCheckChangeMs = now;

      if (lastCheckStable == LOW) {
        // 검수센서 LOW 감지 시 파이썬으로 신호 전달
        Serial.println("CHECK_0");
      } else {
        Serial.println("CHECK_1");
      }
    }
  } else {
    lastCheckChangeMs = now;
  }


  // 3) 파이썬 명령 수신 처리
  while (Serial.available()) {
    char c = (char)Serial.read();
    if (c == '\r') continue;
    if (c == '\n') {
      if (rxBuf.length() > 0) {
        handleCommand(rxBuf);
        rxBuf = "";
      }
    } else {
      rxBuf += c;
      if (rxBuf.length() > 64) rxBuf = "";
    }
  }

  delay(1);
}
