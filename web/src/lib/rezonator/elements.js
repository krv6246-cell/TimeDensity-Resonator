export const ELEMENTS = {
  H: { symbol: "H", name: "Водород", freq: 659, color: "#22d3ee", rgb: "34,211,238" },
  He: { symbol: "He", name: "Гелий", freq: 392, color: "#a78bfa", rgb: "167,139,250" },
  C: { symbol: "C", name: "Углерод", freq: 293, color: "#fbbf24", rgb: "251,191,36" },
  N: { symbol: "N", name: "Азот", freq: 261, color: "#60a5fa", rgb: "96,165,250" },
  O: { symbol: "O", name: "Кислород", freq: 220, color: "#f97316", rgb: "249,115,22" },
  Fe: { symbol: "Fe", name: "Железо", freq: 110, color: "#f43f5e", rgb: "244,63,94" },
};

export const ELEMENT_KEYS = Object.keys(ELEMENTS);

export const ELEMENT_LIST = ELEMENT_KEYS.map((key) => ({ key, ...ELEMENTS[key] }));