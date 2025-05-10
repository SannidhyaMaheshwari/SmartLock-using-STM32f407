#define RCC_AHB1ENR  (*(volatile unsigned int*)0x40023830)
#define RCC_APB1ENR  (*(volatile unsigned int*)0x40023840)
#define GPIOA_MODER  (*(volatile unsigned int*)0x40020000)
#define GPIOA_AFRL   (*(volatile unsigned int*)0x40020020)
#define TIM3_PSC     (*(volatile unsigned int*)0x40000428)
#define TIM3_ARR     (*(volatile unsigned int*)0x4000042C)
#define TIM3_CCR1    (*(volatile unsigned int*)0x40000434)
#define TIM3_CCMR1   (*(volatile unsigned int*)0x40000418)
#define TIM3_CCER    (*(volatile unsigned int*)0x40000420)
#define TIM3_CR1     (*(volatile unsigned int*)0x40000400)
#define TIM3_EGR     (*(volatile unsigned int*)0x40000414)

void Servo_Init(void) {
    RCC_AHB1ENR |= (1 << 0); // GPIOA
    RCC_APB1ENR |= (1 << 1); // TIM3

    GPIOA_MODER &= ~(3U << (6 * 2));
    GPIOA_MODER |=  (2U << (6 * 2));
    GPIOA_AFRL  &= ~(0xF << (6 * 4));
    GPIOA_AFRL  |=  (2U << (6 * 4)); // AF2

    TIM3_PSC = 1599;   // 10kHz
    TIM3_ARR = 200;    // 50Hz
    TIM3_CCR1 = 5;     // 0°
    TIM3_CCMR1 |= (6U << 4) | (1 << 3);
    TIM3_CCER |= 1;
    TIM3_CR1  |= (1 << 7);
    TIM3_EGR  |= 1;
    TIM3_CR1  |= 1;
}

void Servo_SetAngle(int angle) {
    if (angle < 0) angle = 0;
    if (angle > 180) angle = 180;
    TIM3_CCR1 = 5 + ((angle * 10) / 180);
}
