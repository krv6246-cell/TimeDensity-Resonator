import React from "react";
import { motion } from "framer-motion";

const WORDS = ["Tune.", "Hold.", "Capture."];

export default function OnboardingHint() {
  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
      className="pointer-events-none absolute inset-0 z-20 flex flex-col items-center justify-center"
    >
      <div className="flex items-baseline gap-3">
        {WORDS.map((word, i) => (
          <motion.span
            key={word}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.18 * i, duration: 0.5 }}
            className="text-lg font-light tracking-[0.24em] text-white/90 sm:text-2xl"
          >
            {word}
          </motion.span>
        ))}
      </div>
      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.7, duration: 0.6 }}
        className="mt-4 text-[10px] tracking-[0.42em] text-cyan-300/70"
      >
        RESONANCE PROTOCOL
      </motion.p>
    </motion.div>
  );
}