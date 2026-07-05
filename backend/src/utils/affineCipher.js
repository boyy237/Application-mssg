/**
 * Chiffrement affine — repris de la démo web originale (CipherTalk).
 * E(x) = (a·x + b) mod 26      D(y) = a⁻¹·(y − b) mod 26
 *
 * Le serveur ne s'en sert que pour valider qu'une clé (a, b) est correcte
 * avant de l'enregistrer sur une conversation : le chiffrement / déchiffrement
 * réel des messages est effectué côté client (Android), le serveur ne stocke
 * et ne transporte que le texte déjà chiffré.
 */

const M = 26;

function gcd(a, b) {
  return b === 0 ? a : gcd(b, a % b);
}

function extGcd(a, b) {
  if (b === 0) return [a, 1, 0];
  const [g, x1, y1] = extGcd(b, a % b);
  return [g, y1, x1 - Math.floor(a / b) * y1];
}

function modInverse(a, m) {
  const [g, x] = extGcd(((a % m) + m) % m, m);
  if (g !== 1) throw new Error(`Pas d'inverse pour a=${a} mod ${m}`);
  return ((x % m) + m) % m;
}

function isValidKey(a, b) {
  return Number.isInteger(a) && Number.isInteger(b) &&
    gcd(((a % M) + M) % M, M) === 1 && b >= 0 && b <= M - 1;
}

module.exports = { M, gcd, modInverse, isValidKey };
