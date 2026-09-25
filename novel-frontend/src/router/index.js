import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Detail from '../views/Detail.vue'
import Read from '../views/Read.vue'
import Admin from '../views/Admin.vue'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: Home
    },
    {
        path: '/novel/:id',
        name: 'Detail',
        component: Detail
    },
    {
        path: '/chapter/:id',
        name: 'Read',
        component: Read
    },
    {
        path: '/admin/export',
        name: 'AdminExport',
        component: Admin
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
