import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Pompeia Runners',
  description: 'Web application for Pompeia Runners',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  )
}
