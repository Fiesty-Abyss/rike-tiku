import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081/api/v1',
  timeout: 5000,
  headers: {
    Accept: 'application/json',
  },
})

export default http

