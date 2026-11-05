import type { EstadoReserva, Motor } from './tipos'

export const NOMBRE_MOTOR: Record<Motor, string> = { mysql: 'MySQL', mongo: 'MongoDB' }

export const ESTADOS: Record<EstadoReserva, string> = {
  PENDIENTE: 'Pendiente',
  CONFIRMADA: 'Confirmada',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
}

const REGIMENES: Record<string, string> = {
  'Breakfast Included': 'Desayuno incluido',
  'Full Board': 'Pensión completa',
  'Half Board': 'Media pensión',
  'Room Only': 'Solo alojamiento',
}

const MEDIOS_PAGO: Record<string, string> = {
  'Credit Card': 'Tarjeta de crédito',
  'Debit Card': 'Tarjeta de débito',
  'Digital Wallet': 'Billetera virtual',
  EMI: 'Cuotas',
  'Net Banking': 'Transferencia bancaria',
  UPI: 'Pago instantáneo (UPI)',
}

const TRANSPORTES: Record<string, string> = { Flight: 'Avión', Train: 'Tren', Car: 'Auto' }

const GENEROS: Record<string, string> = { Male: 'Masculino', Female: 'Femenino' }

const PAISES: Record<string, string> = {
  Australia: 'Australia',
  Canada: 'Canadá',
  France: 'Francia',
  India: 'India',
  Indonesia: 'Indonesia',
  Malaysia: 'Malasia',
  Maldives: 'Maldivas',
  Nepal: 'Nepal',
  Singapore: 'Singapur',
  'Sri Lanka': 'Sri Lanka',
  Thailand: 'Tailandia',
  'United Arab Emirates': 'Emiratos Árabes Unidos',
  'United Kingdom': 'Reino Unido',
  'United States': 'Estados Unidos',
}

const MOTIVOS: Record<string, string> = {
  'Budget constraints': 'Problemas de presupuesto',
  'Change in travel plans': 'Cambio de planes',
  'Family emergency': 'Emergencia familiar',
  'Medical emergency': 'Emergencia médica',
  'Transportation disruption': 'Problemas con el transporte',
  'Visa issue': 'Problema con la visa',
  'Work commitment': 'Compromiso laboral',
}

const traducir = (tabla: Record<string, string>) => (valor: string) => tabla[valor] ?? valor

export const regimen = traducir(REGIMENES)
export const medioPago = traducir(MEDIOS_PAGO)
export const transporte = traducir(TRANSPORTES)
export const genero = traducir(GENEROS)
export const pais = traducir(PAISES)
export const motivo = traducir(MOTIVOS)

const formatoPlata = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 })
const formatoNumero = new Intl.NumberFormat('es-AR')
const formatoDecimal = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 2 })
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

export const plata = (valor: number) => formatoPlata.format(valor).replace(/\s/, ' ')
export const numero = (valor: number) => formatoNumero.format(valor)
export const decimal = (valor: number) => formatoDecimal.format(valor)
export const fechaCorta = (iso: string) => `${Number(iso.slice(8, 10))} ${MESES[Number(iso.slice(5, 7)) - 1]}`
export const fecha = (iso: string) => `${fechaCorta(iso)} ${iso.slice(0, 4)}`
export const nroReserva = (id: number) => `N° ${String(id).padStart(4, '0')}`
export const noches = (cantidad: number) => `${cantidad} ${cantidad === 1 ? 'noche' : 'noches'}`
export const personas = (adultos: number, ninos: number) =>
  `${adultos} ${adultos === 1 ? 'adulto' : 'adultos'}${ninos > 0 ? ` y ${ninos} ${ninos === 1 ? 'niño' : 'niños'}` : ''}`
