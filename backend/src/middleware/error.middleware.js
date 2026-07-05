function notFound(req, res, next) {
  res.status(404).json({ error: `Route non trouvée : ${req.method} ${req.originalUrl}` });
}

function errorHandler(err, req, res, next) {
  console.error(err);
  const status = err.status || 500;
  res.status(status).json({ error: err.message || 'Erreur interne du serveur.' });
}

module.exports = { notFound, errorHandler };
