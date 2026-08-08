export interface PollingTimers {
  setInterval(handler: () => void, timeout: number): ReturnType<typeof setInterval>
  clearInterval(id: ReturnType<typeof setInterval>): void
}

export function startMessagePolling(
  load: () => void | Promise<void>,
  interval = 7000,
  timers: PollingTimers = globalThis,
) {
  const id = timers.setInterval(() => void load(), interval)
  return () => timers.clearInterval(id)
}
