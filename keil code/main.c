#include <string.h>

// RCC & GPIO Register Definitions
#define RCC_AHB1ENR     (*(volatile unsigned int*)0x40023830)
#define RCC_APB1ENR     (*(volatile unsigned int*)0x40023840)
#define GPIOA_MODER     (*(volatile unsigned int*)0x40020000)
#define GPIOA_AFRL      (*(volatile unsigned int*)0x40020020)
#define GPIOA_OSPEEDR   (*(volatile unsigned int*)0x40020008)
#define GPIOD_MODER     (*(volatile unsigned int*)0x40020C00)
#define GPIOD_ODR       (*(volatile unsigned int*)0x40020C14)

// USART2 Registers
#define USART2_SR       (*(volatile unsigned int*)0x40004400)
#define USART2_DR       (*(volatile unsigned int*)0x40004404)
#define USART2_BRR      (*(volatile unsigned int*)0x40004408)
#define USART2_CR1      (*(volatile unsigned int*)0x4000440C)

#define USART_SR_RXNE   (1U << 5)
#define USART_SR_TXE    (1U << 7)

// LED Pins
#define GREEN_LED 12
#define RED_LED   14

// Function Prototypes
void Keypad_Init(void);
char Keypad_GetKey(void);
void Servo_Init(void);
void Servo_SetAngle(int angle);
void LED_Init(void);
void blink_led(int pin, int times);
void clear_input(char *input, int *index);
void UART2_Init(void);
void UART2_SendChar(char c);
char UART2_ReadChar(void);
void red_led_blink_timeout(void);

// Global state
static int servo_state = 0;
static const char permanent_password[5] = "1234";
static char temp_password[5] = "";
static int temp_uses = 0;
static int invalid_attempts = 0;
static int lockout = 0;
static int receiving_temp = 0;

void UART2_Init(void) {
    RCC_AHB1ENR |= (1 << 0);
    RCC_APB1ENR |= (1 << 17);

    GPIOA_MODER &= ~((3U << (2 * 2)) | (3U << (3 * 2)));
    GPIOA_MODER |= ((2U << (2 * 2)) | (2U << (3 * 2)));

    GPIOA_AFRL &= ~((0xFU << (4 * 2)) | (0xFU << (4 * 3)));
    GPIOA_AFRL |= ((7U << (4 * 2)) | (7U << (4 * 3)));

    GPIOA_OSPEEDR |= ((3U << (2 * 2)) | (3U << (3 * 2)));

    USART2_BRR = 0x683;
    USART2_CR1 = (1 << 13) | (1 << 3) | (1 << 2);
}

void UART2_SendChar(char c) {
    while (!(USART2_SR & USART_SR_TXE));
    USART2_DR = c & 0xFF;
}

char UART2_ReadChar(void) {
    if (USART2_SR & USART_SR_RXNE)
        return (char)(USART2_DR & 0xFF);
    return '\0';
}

void LED_Init(void) {
    RCC_AHB1ENR |= (1 << 3);
    GPIOD_MODER |= (1U << (GREEN_LED * 2));
    GPIOD_MODER |= (1U << (RED_LED * 2));
}

void blink_led(int pin, int times) {
    for (int i = 0; i < times; i++) {
        GPIOD_ODR |= (1U << pin);
        for (volatile int d = 0; d < 300000; d++);
        GPIOD_ODR &= ~(1U << pin);
        for (volatile int d = 0; d < 300000; d++);
    }
}

void red_led_blink_timeout(void) {
    for (volatile long i = 0; i < 3000; i++) {
        GPIOD_ODR |= (1U << RED_LED);
        for (volatile int d = 0; d < 300000; d++);
        GPIOD_ODR &= ~(1U << RED_LED);
        for (volatile int d = 0; d < 300000; d++);
    }
}

void clear_input(char *input, int *index) {
    for (int i = 0; i < 5; i++) input[i] = '\0';
    *index = 0;
}

int main(void) {
    Keypad_Init();
    LED_Init();
    Servo_Init();
    UART2_Init();
    blink_led(GREEN_LED, 2);

    char input[5] = "";
    int index = 0;

    Servo_SetAngle(0);
    servo_state = 0;
    GPIOD_ODR &= ~(1U << GREEN_LED);
    GPIOD_ODR &= ~(1U << RED_LED);

    while (1) {
        char incoming = '\0';
        if (!receiving_temp && index == 0) {
            incoming = UART2_ReadChar();
        }

        if (incoming == 'L' || incoming == 'U') {
            int desired = (incoming == 'U') ? 1 : 0;
            if (desired != servo_state) {
                servo_state = desired;
                Servo_SetAngle(desired ? 180 : 0);
                if (desired)
                    GPIOD_ODR |= (1U << GREEN_LED);
                else
                    GPIOD_ODR &= ~(1U << GREEN_LED);
            }
            UART2_SendChar('A');
            incoming = '\0';
        }

        else if (incoming == 'T') {
            receiving_temp = 1;
            char tempBuf[5] = "";
            int count = 0;
            while (count < 4) {
                while (!(USART2_SR & USART_SR_RXNE));
                char c = (char)(USART2_DR & 0xFF);
                if (c == '\n' || c == '\r') continue;
                tempBuf[count++] = c;
            }
            tempBuf[4] = '\0';
            strncpy(temp_password, tempBuf, 5);
            temp_uses = 2;
            invalid_attempts = 0;
            clear_input(input, &index);
            for (volatile int i = 0; i < 2000000; i++);
            while (USART2_SR & USART_SR_RXNE) {
                volatile char dump = (char)(USART2_DR);
            }
            blink_led(RED_LED, 2);
            blink_led(GREEN_LED, 1);
            incoming = '\0';
            receiving_temp = 0;
        }

        char key = Keypad_GetKey();
        if (lockout || receiving_temp) continue;

        if (key != '\0') {
            if (key >= '0' && key <= '9' && index < 4) {
                input[index++] = key;
                blink_led(GREEN_LED, 1);
            } else if (key == 'A') {
                input[4] = '\0';

                // ? TEMP PASSWORD MATCH
                if (strcmp(input, temp_password) == 0 && temp_uses > 0) {
                    temp_uses--;
                    invalid_attempts = 0; // ? FIX HERE
                    if (temp_uses == 0) temp_password[0] = '\0';
                    blink_led(GREEN_LED, 2);
                    servo_state = !servo_state;
                    Servo_SetAngle(servo_state ? 180 : 0);
                    GPIOD_ODR = (GPIOD_ODR & ~(1U << GREEN_LED)) |
                                (servo_state ? (1U << GREEN_LED) : 0);
                    UART2_SendChar(servo_state ? 'U' : 'L');
                    for (volatile int i = 0; i < 1000000; i++) {
                        if (UART2_ReadChar() == 'A') break;
                    }
                }

                // ? PERMANENT PASSWORD MATCH
                else if (strcmp(input, permanent_password) == 0) {
                    invalid_attempts = 0; // ? FIX HERE
                    servo_state = !servo_state;
                    Servo_SetAngle(servo_state ? 180 : 0);
                    GPIOD_ODR = (GPIOD_ODR & ~(1U << GREEN_LED)) |
                                (servo_state ? (1U << GREEN_LED) : 0);
                    UART2_SendChar(servo_state ? 'U' : 'L');
                    for (volatile int i = 0; i < 1000000; i++) {
                        if (UART2_ReadChar() == 'A') break;
                    }
                }

                // ? WRONG PASSWORD
                else {
                    invalid_attempts++;
                    blink_led(RED_LED, 2);
                    GPIOD_ODR &= ~(1U << GREEN_LED);
                    if (invalid_attempts >= 3) {
                        lockout = 1;
                        red_led_blink_timeout();
                        lockout = 0;
                        invalid_attempts = 0;
                    }
                }

                clear_input(input, &index);
            } else if (key == 'B') {
                clear_input(input, &index);
                blink_led(GREEN_LED, 3);
                GPIOD_ODR &= ~(1U << GREEN_LED);
                GPIOD_ODR &= ~(1U << RED_LED);
            }
        }
    }
}
