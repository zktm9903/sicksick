type ClassValue = string | false | null | undefined

/** CSS Modules 클래스명을 조건부로 합친다. */
export function cx(...values: ClassValue[]): string {
  return values.filter(Boolean).join(' ')
}
