module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    // Tipos de commits permitidos
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'docs', 'chore', 'refactor', 'test']
    ],
    // 2. Módulos (scopes) permitidos 
    'scope-enum': [
      2,
      'always',
      [
        'checkin',
        'reservas',
        'pagos',
        'tracking',
        'administracion',
        'reportes',
        'cuentas',
        'busqueda',
        'integracion',
        'core' // generales (seguridad, db, etc.)
      ]
    ],

    'scope-empty': [2, 'never']
  }
};