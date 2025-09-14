package net.shibori.meiwei.controller

import net.shibori.meiwei.dto.RepositoryFormDto
import net.shibori.meiwei.service.RepositoryService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import jakarta.validation.Valid

@Controller
@RequestMapping("/repositories")
class RepositoryController(
    private val repositoryService: RepositoryService
) {

    // リポジトリ一覧表示
    @GetMapping
    fun list(model: Model, @RequestParam(required = false) keyword: String?): String {
        val repositories = if (keyword.isNullOrBlank()) {
            repositoryService.getActiveRepositories()
        } else {
            repositoryService.searchRepositories(keyword)
            model.addAttribute("keyword", keyword)
        }

        model.addAttribute("repositories", repositories)
        return "repositories/list"
    }

    // リポジトリ作成フォーム表示
    @GetMapping("/new")
    fun showCreateForm(model: Model): String {
        model.addAttribute("repositoryForm", RepositoryFormDto())
        return "repositories/form"
    }

    // リポジトリ作成処理
    @PostMapping("/new")
    fun create(
        @Valid @ModelAttribute("repositoryForm") formDto: RepositoryFormDto,
        bindingResult: BindingResult,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            return "repositories/form"
        }

        try {
            val repository = repositoryService.createRepository(formDto)
            redirectAttributes.addFlashAttribute("successMessage", "リポジトリ「${repository.name}」を作成しました。")
            return "redirect:/repositories/${repository.id}"
        } catch (e: IllegalArgumentException) {
            bindingResult.rejectValue("name", "error.duplicate", e.message ?: "")
            return "repositories/form"
        }
    }

    // リポジトリ詳細表示
    @GetMapping("/{id}")
    fun show(@PathVariable id: Long, model: Model): String {
        try {
            val repository = repositoryService.getRepository(id)
            model.addAttribute("repository", repository)
            return "repositories/detail"
        } catch (e: IllegalArgumentException) {
            return "redirect:/repositories?error=notfound"
        }
    }

    // リポジトリ編集フォーム表示
    @GetMapping("/{id}/edit")
    fun showEditForm(@PathVariable id: Long, model: Model): String {
        try {
            val repository = repositoryService.getRepository(id)
            val formDto = RepositoryFormDto(
                name = repository.name,
                cloneUrl = repository.cloneUrl,
                branchName = repository.branchName,
                description = repository.description
            )
            model.addAttribute("repositoryForm", formDto)
            model.addAttribute("repositoryId", id)
            return "repositories/form"
        } catch (e: IllegalArgumentException) {
            return "redirect:/repositories?error=notfound"
        }
    }

    // リポジトリ更新処理
    @PostMapping("/{id}/edit")
    fun update(
        @PathVariable id: Long,
        @Valid @ModelAttribute("repositoryForm") formDto: RepositoryFormDto,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            model.addAttribute("repositoryId", id)
            return "repositories/form"
        }

        try {
            val repository = repositoryService.updateRepository(id, formDto)
            redirectAttributes.addFlashAttribute("successMessage", "リポジトリ「${repository.name}」を更新しました。")
            return "redirect:/repositories/${id}"
        } catch (e: IllegalArgumentException) {
            bindingResult.rejectValue("name", "error.duplicate", e.message ?: "")
            model.addAttribute("repositoryId", id)
            return "repositories/form"
        }
    }

    // リポジトリ削除処理
    @PostMapping("/{id}/delete")
    fun delete(@PathVariable id: Long, redirectAttributes: RedirectAttributes): String {
        try {
            repositoryService.deactivateRepository(id)
            redirectAttributes.addFlashAttribute("successMessage", "リポジトリを削除しました。")
        } catch (e: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除対象のリポジトリが見つかりません。")
        }
        return "redirect:/repositories"
    }

    // 除外設定ページへのリダイレクト
    @GetMapping("/{id}/exclusions")
    fun redirectToExclusions(@PathVariable id: Long): String {
        return "redirect:/exclusions?repositoryId=$id"
    }

    // レビュー実行ページへのリダイレクト
    @GetMapping("/{id}/review")
    fun redirectToReview(@PathVariable id: Long): String {
        return "redirect:/reviews/execute?repositoryId=$id"
    }
}