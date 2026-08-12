export async function executePasswordRecovery<T>(
  confirm: () => Promise<unknown>,
  recover: () => Promise<T>,
): Promise<T | null> {
  try {
    await confirm()
  } catch {
    return null
  }
  return recover()
}
