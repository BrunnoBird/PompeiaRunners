'use client'

import { useState } from 'react'
import { JSLogo } from '../JSLogo/JSLogo'
import { Greeting as KotlinGreeting } from 'shared'
import type { AnimationEvent } from 'react'

export function Greeting() {
  const greeting = new KotlinGreeting()
  const [isVisible, setIsVisible] = useState<boolean>(false)
  const [isAnimating, setIsAnimating] = useState<boolean>(false)

  const handleClick = () => {
    if (isVisible) {
      setIsAnimating(true)
    } else {
      setIsVisible(true)
    }
  }

  const handleAnimationEnd = (event: AnimationEvent<HTMLDivElement>) => {
    if (event.animationName === 'fadeOut') {
      setIsVisible(false)
      setIsAnimating(false)
    }
  }

  return (
    <div className="flex flex-col items-center gap-6 p-8">
      <button
        onClick={handleClick}
        className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
      >
        Click me!
      </button>

      {isVisible && (
        <div
          className={isAnimating ? 'animate-fade-out' : 'animate-fade-in'}
          onAnimationEnd={handleAnimationEnd}
        >
          <JSLogo />
          <div className="mt-4 text-lg font-medium">React: {greeting.greet()}</div>
        </div>
      )}
    </div>
  )
}
