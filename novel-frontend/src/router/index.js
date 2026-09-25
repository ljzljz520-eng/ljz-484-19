import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Detail from '../views/Detail.vue'
import Read from '../views/Read.vue'

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
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
