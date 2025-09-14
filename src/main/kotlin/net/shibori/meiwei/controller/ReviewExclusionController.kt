package net.shibori.meiwei.controller

import net.shibori.meiwei.dto.ReviewExclusionFormDto
import net.shibori.meiwei.entity.ReviewExclusion
import net.shibori.meiwei.enums.ExclusionType
import net.shibori.meiwei.repository.ReviewExclusionRepository
import net.shibori.meiwei.repository.RepositoryRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import jakarta.validation.Valid

@Controller
@RequestMapping("/exclusions")
class ReviewExclusionController(
    private val reviewExclusionRepository: ReviewExclusionRepository,
    private val repositoryRepository: RepositoryRepository
) {

    // 除外設定一覧表示
    @GetMapping
    fun list(@RequestParam repositoryId: Long, model: Model): String {
        try {
            val repository = repositoryRepository.findById(repositoryId).orElseThrow {
                IllegalArgumentException("リポジトリが見つかりません")
            }

            val exclusions = reviewExclusionRepository.findByRepositoryIdAndIsActiveTrueOrderByExclusionTypeAscPathAsc(repositoryId)

            model.addAttribute("repository", repository)
            model.addAttribute("exclusions", exclusions)
            model.addAttribute("exclusionTypes", ExclusionType.values())

            return "exclusions/list"
        } catch (e: IllegalArgumentException) {
            return "redirect:/repositories?error=notfound"
        }
    }

    // 除外設定作成フォーム表示
    @GetMapping("/new")
    fun showCreateForm(@RequestParam repositoryId: Long, model: Model): String {
        try {
            val repository = repositoryRepository.findById(repositoryId).orElseThrow {
                IllegalArgumentException("リポジトリが見つかりません")
            }

            val formDto = ReviewExclusionFormDto(repositoryId = repositoryId)

            model.addAttribute("repository", repository)
            model.addAttribute("exclusionForm", formDto)
            model.addAttribute("exclusionTypes", ExclusionType.values())

            return "exclusions/form"
        } catch (e: IllegalArgumentException) {
            return "redirect:/repositories?error=notfound"
        }
    }

    // 除外設定作成処理
    @PostMapping("/new")
    fun create(
        @Valid @ModelAttribute("exclusionForm") formDto: ReviewExclusionFormDto,
        bindingResult: BindingResult,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        if (bindingResult.hasErrors()) {
            val repository = repositoryRepository.findById(formDto.repositoryId).get()
            model.addAttribute("repository", repository)
            model.addAttribute("exclusionTypes", ExclusionType.values())
            return "exclusions/form"
        }

        try {
            val repository = repositoryRepository.findById(formDto.repositoryId).orElseThrow {
                IllegalArgumentException("リポジトリが見つかりません")
            }

            // 重複チェック
            reviewExclusionRepository.findByRepositoryIdAndPathAndIsActiveTrue(formDto.repositoryId, formDto.path)?.let {
                bindingResult.rejectValue("path", "error.duplicate", "同じパスの除外設定が既に存在します")
                model.addAttribute("repository", repository)
                model.addAttribute("exclusionTypes", ExclusionType.values())
                return "exclusions/form"
            }

            val exclusion = ReviewExclusion(
                repository = repository,
                exclusionType = ExclusionType.valueOf(formDto.exclusionType),
                path = formDto.path,
                pattern = formDto.pattern,
                description = formDto.description
            )

            reviewExclusionRepository.save(exclusion)
            redirectAttributes.addFlashAttribute("successMessage", "除外設定を作成しました。")

            return "redirect:/exclusions?repositoryId=${formDto.repositoryId}"
        } catch (e: Exception) {
            bindingResult.reject("error.general", "除外設定の作成に失敗しました: ${e.message}")
            val repository = repositoryRepository.findById(formDto.repositoryId).get()
            model.addAttribute("repository", repository)
            model.addAttribute("exclusionTypes", ExclusionType.values())
            return "exclusions/form"
        }
    }

    // 除外設定削除処理
    @PostMapping("/{id}/delete")
    fun delete(@PathVariable id: Long, redirectAttributes: RedirectAttributes): String {
        try {
            val exclusion = reviewExclusionRepository.findById(id).orElseThrow {
                IllegalArgumentException("除外設定が見つかりません")
            }

            val repositoryId = exclusion.repository.id!!
            val updated = exclusion.copy(isActive = false)
            reviewExclusionRepository.save(updated)

            redirectAttributes.addFlashAttribute("successMessage", "除外設定を削除しました。")
            return "redirect:/exclusions?repositoryId=$repositoryId"
        } catch (e: IllegalArgumentException) {
            redirectAttributes.addFlashAttribute("errorMessage", "削除対象の除外設定が見つかりません。")
            return "redirect:/repositories"
        }
    }

    // レビュー実行へ進む
    @GetMapping("/next")
    fun nextToReview(@RequestParam repositoryId: Long): String {
        return "redirect:/reviews/execute?repositoryId=$repositoryId"
    }
}