package domain.model

data class ProjectDetails(
    val project: Project,
    val story: String,
    val risks: String?,
    val rewards: List<Reward>,
    val environmentalCommitments: List<String>,
    val faqCount: Int,
    val commentsCount: Int,
    val updatesCount: Int,
    val hasVideo: Boolean
)