export function isMockMode() {
    return import.meta.env.MODE === 'mock'
}


export function isMockToken(token) {
    return typeof token === 'string' && token.startsWith('mock.')
}


export function mockToken(username) {
    // just a placeholder string; backend isn't called in mock mode
    return `mock.${btoa(username)}.${Date.now()}`
}