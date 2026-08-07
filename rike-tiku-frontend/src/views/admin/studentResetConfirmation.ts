export async function confirmStudentPasswordReset(confirm: () => Promise<unknown>) {
  try {
    await confirm()
    return true
  } catch {
    return false
  }
}
