package net.shibori.meiwei.controller

import net.shibori.meiwei.service.RepositoryService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(
    private val repositoryService: RepositoryService
) {

    @GetMapping("/")
    fun index(model: Model): String {
        val repositories = repositoryService.getActiveRepositories()
        model.addAttribute("repositories", repositories)
        return "index"
    }
}