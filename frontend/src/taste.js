const AXES = [
  { key: "spice", label: "辣" },
  { key: "salt", label: "咸" },
  { key: "light", label: "清淡" },
];

export { AXES };

export function inferTaste({ name = "", note = "", tags = "" } = {}) {
  const text = [name, note, tags].join(" ");
  let spice = 1;
  if (/变态辣|特辣|很辣|麻辣|香辣|辣子|水煮/.test(text)) spice = 5;
  else if (/中辣|重辣|辣/.test(text)) spice = 4;
  else if (/微辣|少辣/.test(text)) spice = 2;
  else if (/不辣|免辣/.test(text)) spice = 0;

  let salt = 2;
  if (/重口|很咸|咸香|下饭/.test(text)) salt = 5;
  else if (/偏咸|咸/.test(text)) salt = 4;
  else if (/少盐|清淡/.test(text)) salt = 1;

  let light = 2;
  if (/清淡|少油|蒸|白灼|养生/.test(text)) light = 5;
  else if (/油腻|干锅|红烧|炸/.test(text)) light = 1;

  return { spice, salt, light };
}

export function tasteOf(dish) {
  if (!dish) return { spice: 0, salt: 0, light: 0 };
  const stored = dish.spice + dish.salt + dish.light;
  if (stored > 0) {
    return { spice: dish.spice ?? 0, salt: dish.salt ?? 0, light: dish.light ?? 0 };
  }
  return inferTaste(dish);
}

export function averageTaste(dishes) {
  if (!dishes.length) return { spice: 0, salt: 0, light: 0 };
  const sum = dishes.reduce(
    (acc, dish) => {
      const t = tasteOf(dish);
      acc.spice += t.spice;
      acc.salt += t.salt;
      acc.light += t.light;
      return acc;
    },
    { spice: 0, salt: 0, light: 0 },
  );
  const n = dishes.length;
  return {
    spice: Math.round((sum.spice / n) * 10) / 10,
    salt: Math.round((sum.salt / n) * 10) / 10,
    light: Math.round((sum.light / n) * 10) / 10,
  };
}
