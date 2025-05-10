#define RCC_AHB1ENR    (*(volatile unsigned int*)0x40023830)
#define GPIOD_MODER    (*(volatile unsigned int*)0x40020C00)
#define GPIOD_PUPDR    (*(volatile unsigned int*)0x40020C0C)
#define GPIOD_IDR      (*(volatile unsigned int*)0x40020C10)
#define GPIOD_ODR      (*(volatile unsigned int*)0x40020C14)

static unsigned int ROW[4] = {0, 1, 2, 3};
static unsigned int COL[4] = {4, 5, 6, 7};

static const char KEYS[4][4] = {
    {'1','2','3','A'},
    {'4','5','6','B'},
    {'7','8','9','C'},
    {'0','F','E','D'}
};

void Keypad_Init(void) {
    RCC_AHB1ENR |= (1 << 3); // GPIOD

    for (int i = 0; i < 4; i++) {
        GPIOD_MODER &= ~(3U << (ROW[i] * 2));
        GPIOD_PUPDR &= ~(3U << (ROW[i] * 2));
        GPIOD_PUPDR |=  (1U << (ROW[i] * 2)); // pull-up
    }

    for (int i = 0; i < 4; i++) {
        GPIOD_MODER &= ~(3U << (COL[i] * 2));
        GPIOD_MODER |=  (1U << (COL[i] * 2)); // output
    }
}

char Keypad_GetKey(void) {
    for (int col = 0; col < 4; col++) {
        for (int i = 0; i < 4; i++)
            GPIOD_ODR |= (1U << COL[i]);
        GPIOD_ODR &= ~(1U << COL[col]);

        for (volatile int d = 0; d < 1000; d++);

        for (int row = 0; row < 4; row++) {
            if (!(GPIOD_IDR & (1 << ROW[row]))) {
                for (volatile int d = 0; d < 100000; d++);
                if (!(GPIOD_IDR & (1 << ROW[row]))) {
                    while (!(GPIOD_IDR & (1 << ROW[row])));
                    return KEYS[row][col];
                }
            }
        }
    }
    return '\0';
}
