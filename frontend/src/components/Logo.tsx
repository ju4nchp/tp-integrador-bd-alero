export function MarcaAlero({ tamanio = 34 }: { tamanio?: number }) {
  return (
    <svg width={tamanio} height={tamanio} viewBox="0 0 48 48" aria-hidden="true">
      <rect width="48" height="48" rx="12" fill="#ffffff" />
      <path d="M7 25.5 24 11l17 14.5" fill="none" stroke="#1b2a4a" strokeWidth="5" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M17.5 38V29a6.5 6.5 0 0 1 13 0v9" fill="none" stroke="#3d38a3" strokeWidth="4.5" strokeLinecap="round" />
    </svg>
  )
}

export function Logo() {
  return (
    <span className="logo">
      <MarcaAlero />
      <span className="logo__texto">alero</span>
    </span>
  )
}
