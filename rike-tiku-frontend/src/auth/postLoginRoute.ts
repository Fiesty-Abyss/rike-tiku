export function resolvePostLoginPath(
  mustChangePassword: boolean,
  roleCount: number,
  defaultHome: string,
) {
  if (mustChangePassword) return '/change-initial-password'
  if (roleCount > 1) return '/select-role'
  return defaultHome
}
