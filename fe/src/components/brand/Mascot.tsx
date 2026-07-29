type MascotProps = {
  size?: number
}

/** 씩씩이 마스코트. */
export function Mascot({ size = 120 }: MascotProps) {
  return (
    <svg
      viewBox="0 0 120 120"
      width={size}
      height={size}
      role="img"
      aria-label="씩씩이 마스코트"
    >
      <ellipse cx="60" cy="106" rx="30" ry="4" fill="rgba(31,74,51,0.12)" />
      <path
        d="M60 40C86 40 96 62 92 82 89 98 74 108 60 108 46 108 31 98 28 82 24 62 34 40 60 40Z"
        fill="#A3D0AC"
        stroke="#33754F"
        strokeWidth="3.4"
        strokeLinejoin="round"
      />
      <ellipse cx="46" cy="56" rx="9" ry="6" fill="rgba(255,255,255,0.5)" />
      <ellipse cx="43" cy="76" rx="6" ry="3.4" fill="#F2A0A0" opacity="0.55" />
      <ellipse cx="77" cy="76" rx="6" ry="3.4" fill="#F2A0A0" opacity="0.55" />
      <circle cx="51.5" cy="60" r="5.2" fill="#fff" stroke="#33754F" strokeWidth="1.6" />
      <circle cx="52.3" cy="61" r="2.6" fill="#1C2418" />
      <circle cx="68.5" cy="60" r="5.2" fill="#fff" stroke="#33754F" strokeWidth="1.6" />
      <circle cx="69.3" cy="61" r="2.6" fill="#1C2418" />
      <path
        d="M54 80Q60 85 66 80"
        stroke="#33754F"
        strokeWidth="2.6"
        fill="none"
        strokeLinecap="round"
      />
    </svg>
  )
}
