# Smart Lock System

An end-to-end IoT-based smart lock using STM32 (bare-metal), ESP8266, FastAPI, and an Android app. Supports keypad, mobile control, and facial recognition with real-time synchronization and servo-based locking.

## Features

### STM32 (Bare-Metal)
- 4x4 matrix keypad input (A = submit, B = clear)
- Permanent and 2-use temporary password support
- Lockout after 3 incorrect attempts
- Servo motor control via TIM3 (0° = locked, 180° = unlocked)
- LED indicators: green (success), red (error/lockout)
- UART communication with ESP8266 (via USART2)

### ESP8266 (Wi-Fi)
- Connects to FastAPI backend
- Polls `/lock_status` and `/temp_password`
- Sends commands (`L`, `U`, `Txxxx`) via UART to STM32
- Waits for acknowledgment `A` from STM32

### FastAPI (Python)
- RESTful APIs: `/lock`, `/unlock`, `/lock_status`
- Temporary password endpoint: `/set_temp_password`
- Face recognition via `/verify/` (MTCNN + InceptionResnetV1)
- Tracks lock state and temp password usage

### Android App
- Login via credentials or face recognition
- Lock/unlock controls via API
- Temporary password input
- Log history view with search/filter

## Hardware Used
- STM32F4 board
- SG90 servo motor (PWM on PA6)
- 4x4 keypad (PD0–PD7)
- ESP8266 (NodeMCU)
- Green/Red LEDs
- 5V regulated power supply

## Demo Links
- Full Demo: https://youtu.be/MyqWPbqNmGM  
- Temp Password Demo: https://youtu.be/OYHEDT1h9gI  


## Setup
- STM32: Flash code via Keil, connect servo/keypad/LEDs
- ESP8266: Upload via Arduino IDE, update Wi-Fi/IP
- FastAPI: Run with `uvicorn main:app --reload`
- Android: Build and run from Android Studio


