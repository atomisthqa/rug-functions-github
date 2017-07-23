package com.atomist.rug.function.github.issue

import com.atomist.rug.function.github.GitHubFunction
import com.atomist.rug.spi.Handlers.Status
import com.atomist.rug.spi.annotation.{Parameter, RugFunction, Secret, Tag}
import com.atomist.rug.spi.{AnnotatedRugFunction, FunctionResponse, JsonBodyOption, StringBodyOption}
import com.typesafe.scalalogging.LazyLogging

/**
  * Close a GitHub issue.
  */
class CloseIssueFunction extends AnnotatedRugFunction
  with LazyLogging
  with GitHubFunction {

  @RugFunction(name = "close-github-issue", description = "Reopens a closed GitHub issue",
    tags = Array(new Tag(name = "github"), new Tag(name = "issues")))
  def invoke(@Parameter(name = "issue") number: Int,
             @Parameter(name = "repo") repo: String,
             @Parameter(name = "owner") owner: String,
             @Parameter(name = "apiUrl") apiUrl: String,
             @Secret(name = "user_token", path = "github://user_token?scopes=repo") token: String): FunctionResponse = {

    logger.info(s"Invoking closeIssue with number '$number', owner '$owner', repo '$repo' and token '${safeToken(token)}'")

    try {
      val ghs = gitHubServices(token, apiUrl)
      val issue = ghs.getIssue(repo, owner, number).get
      val assignees = issue.assignees.toList.flatten.map(_.login)
      val response = ghs.editIssue(repo, owner, issue.number, issue.title, issue.body, "closed", issue.labels.map(_.name), assignees)
      FunctionResponse(Status.Success, Some(s"Successfully closed issue `#$number` in `$owner/$repo`"), None, JsonBodyOption(response))
    } catch {
      case e: Exception =>
        val msg = s"Failed to close issue `#$number` in `$owner/$repo`"
        logger.warn(msg, e)
        FunctionResponse(Status.Failure, Some(msg), None, StringBodyOption(e.getMessage))
    }
  }
}
