export function resolvePostLoginPath(
  _mustChangePassword: boolean,
  roleCount: number,
  defaultHome: string,
) {
  if (roleCount > 1) return '/select-role'
  return defaultHome
}
