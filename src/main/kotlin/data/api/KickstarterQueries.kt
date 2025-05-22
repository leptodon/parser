package data.api

object KickstarterQueries {
    fun fetchProjectsQuery() = """
        {
  "operationName": "FetchProjects",
  "variables": {
    "sort": "NEWEST",
    "recommended": true
  },
  "query": "query FetchProjects(${'$'}first: Int = 15 , ${'$'}cursor: String, ${'$'}sort: ProjectSort, ${'$'}state: PublicProjectState, ${'$'}backed: Boolean, ${'$'}recommended: Boolean, ${'$'}categoryId: String, ${'$'}starred: Boolean, ${'$'}staffPicks: Boolean, ${'$'}searchTerm: String) { projects(first: ${'$'}first, after: ${'$'}cursor, sort: ${'$'}sort, backed: ${'$'}backed, recommended: ${'$'}recommended, categoryId: ${'$'}categoryId, starred: ${'$'}starred, state: ${'$'}state, staffPicks: ${'$'}staffPicks, term: ${'$'}searchTerm) { edges { cursor node { __typename ...projectCard } } pageInfo { __typename ...pageInfo } totalCount } }  fragment category on Category { analyticsName id name slug totalProjectCount url parentCategory { analyticsName id name slug totalProjectCount url } }  fragment country on Country { code name }  fragment user on User { name id imageUrl(blur: false, width: 54) isCreator chosenCurrency }  fragment amount on Money { amount currency symbol }  fragment location on Location { displayableName country id name }  fragment full on Project { image { url(width: 1024) altText } }  fragment projectCard on Project { __typename backersCount description isLaunched isPledgeOverTimeAllowed backing { id } category { __typename ...category } country { __typename ...country } createdAt creator { __typename ...user } prelaunchActivated projectNotice projectOfTheDayAt friends(first: 3) { nodes { __typename ...user } } fxRate deadlineAt goal { __typename ...amount } pledged { __typename ...amount } id isWatched launchedAt location { __typename ...location } name ...full prelaunchActivated slug isProjectWeLove state stateChangedAt url isInPostCampaignPledgingPhase postCampaignPledgingEnabled pledgeOverTimeCollectionPlanChargeExplanation pledgeOverTimeCollectionPlanChargedAsNPayments pledgeOverTimeCollectionPlanShortPitch }  fragment pageInfo on PageInfo { hasPreviousPage hasNextPage startCursor endCursor }"
}
""".trimIndent()

    fun fetchProjectDetailsQuery() = """
        {
          "operationName": "FetchProject",
          "variables": {
            "slug": "tototam/cat-spotting"
          },
          "query": "query FetchProject(${'$'}slug: String!, ${'$'}rewardImageWidth: Int = 1024 ) { project(slug: ${'$'}slug) { __typename ...fullProject } }  fragment project on Project { id slug }  fragment amount on Money { amount currency symbol }  fragment location on Location { displayableName country id name }  fragment payment on CreditCard { id lastFour expirationDate type state stripeCardId }  fragment paymentIncrementAmount on PaymentIncrementAmount { amountAsCents amountAsFloat amountFormattedInProjectNativeCurrency amountFormattedInProjectNativeCurrencyWithCurrencyCode currency }  fragment paymentIncrement on PaymentIncrement { id amount { __typename ...paymentIncrementAmount } state stateReason scheduledCollection }  fragment reward on Reward { id name backersCount description estimatedDeliveryOn available amount { __typename ...amount } pledgeAmount { __typename ...amount } latePledgeAmount { __typename ...amount } convertedAmount { __typename ...amount } shippingPreference remainingQuantity limit limitPerBacker startsAt endsAt rewardType allowedAddons { nodes { id } } localReceiptLocation { __typename ...location } }  fragment rewardItems on RewardItemsConnection { edges { quantity node { id name } } }  fragment rewardImage on Reward { image { url(width: ${'$'}rewardImageWidth) altText } }  fragment user on User { name id imageUrl(blur: false, width: 54) isCreator chosenCurrency }  fragment order on Order { id checkoutState currency total }  fragment backing on Backing { id status sequence cancelable pledgedOn backerCompleted backingDetailsPageRoute(type: url, tab: survey_responses) isPostCampaign incremental project { __typename ...project } bonusAmount { __typename ...amount } location { __typename ...location } amount { __typename ...amount } paymentSource { __typename ...payment } paymentIncrements { __typename ...paymentIncrement } shippingAmount { __typename ...amount } reward { __typename ...reward items { __typename ...rewardItems } ...rewardImage } backer { __typename ...user } addOns { nodes { __typename ...reward items { __typename ...rewardItems } ...rewardImage } } order { __typename ...order } }  fragment category on Category { analyticsName id name slug totalProjectCount url parentCategory { analyticsName id name slug totalProjectCount url } }  fragment country on Country { code name }  fragment full on Project { image { url(width: 1024) altText } }  fragment tagsCreative on Project { tags(scope: CREATIVE_PROMPT) { id } }  fragment tagsDiscovery on Project { tags(scope: CREATIVE_PROMPT) { id } }  fragment updates on PostConnection { nodes { updatedAt } totalCount }  fragment video on Video { previewImageUrl videoSources { base { src } high { src } hls { src } } }  fragment faq on ProjectFaq { id answer createdAt question }  fragment aiDisclosure on AiDisclosure { fundingForAiAttribution fundingForAiConsent fundingForAiOption generatedByAiConsent generatedByAiDetails id otherAiDetails }  fragment environmentalCommitment on EnvironmentalCommitment { commitmentCategory description id }  fragment fullProject on Project { __typename availableCardTypes backersCount description minPledge pledgeOverTimeMinimumExplanation isLaunched isPledgeOverTimeAllowed sendMetaCapiEvents sendThirdPartyEvents backing { __typename ...backing } category { __typename ...category } commentsCount(withReplies: true) country { __typename ...country } createdAt creator { __typename ...user } flagging { kind } currency canComment prelaunchActivated projectNotice projectOfTheDayAt friends { nodes { __typename ...user } } fxRate deadlineAt goal { __typename ...amount } id isWatched launchedAt location { __typename ...location } name collaboratorPermissions pledged { __typename ...amount } ...full prelaunchActivated ...tagsCreative ...tagsDiscovery rewards { nodes { __typename ...reward } } risks story slug isProjectWeLove state stateChangedAt usdExchangeRate posts { __typename ...updates } url video { __typename ...video } faqs { nodes { __typename ...faq } } aiDisclosure { __typename ...aiDisclosure } environmentalCommitments { __typename ...environmentalCommitment } watchesCount isInPostCampaignPledgingPhase postCampaignPledgingEnabled pledgeOverTimeCollectionPlanChargeExplanation pledgeOverTimeCollectionPlanChargedAsNPayments pledgeOverTimeCollectionPlanShortPitch }"
        }
    """.trimIndent()

    fun fetchProjectRewardsQuery() = """
        {
          "operationName": "FetchProjectRewards",
          "variables": {
            "slug": "tototam/cat-spotting"
          },
          "query": "query FetchProjectRewards(${'$'}slug: String!, ${'$'}rewardImageWidth: Int = 1024 ) { project(slug: ${'$'}slug) { minPledge rewards { nodes { __typename id ...reward allowedAddons { pageInfo { startCursor } } items { __typename ...rewardItems } simpleShippingRulesExpanded { cost country estimatedMax estimatedMin locationId locationName } ...rewardImage } } } }  fragment amount on Money { amount currency symbol }  fragment location on Location { displayableName country id name }  fragment reward on Reward { id name backersCount description estimatedDeliveryOn available amount { __typename ...amount } pledgeAmount { __typename ...amount } latePledgeAmount { __typename ...amount } convertedAmount { __typename ...amount } shippingPreference remainingQuantity limit limitPerBacker startsAt endsAt rewardType allowedAddons { nodes { id } } localReceiptLocation { __typename ...location } }  fragment rewardItems on RewardItemsConnection { edges { quantity node { id name } } }  fragment rewardImage on Reward { image { url(width: ${'$'}rewardImageWidth) altText } }"
        }
    """.trimIndent()

    fun fetchCreatorDetailsQuery() = """
        {
          "operationName": "ProjectCreatorDetails",
          "variables": {
            "slug": "tototam/cat-spotting"
          },
          "query": "query ProjectCreatorDetails(${'$'}slug: String!) { project(slug: ${'$'}slug) { creator { backingsCount launchedProjects { totalCount } } } }"
        }
    """.trimIndent()
}
