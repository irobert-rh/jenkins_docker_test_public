package com.robinhood

import net.sf.json.JSONArray
import net.sf.json.JSONObject

class PipelineUtils {
    static def getColorAndMentionByBuildResult(buildResult) {
        def color = '#DCDCDC'
        def mention = ''
        switch (buildResult) {
            case 'FAILURE':
                color = 'danger'
                mention = '<!here> '
                break
            case 'SUCCESS':
                color = 'good'
                break
            case 'UNSTABLE':
                color = 'warning'
                mention = '<!here> '
                break
        }
        return [color, mention]
    }

    static def formatAndSendSlackMessage(scriptInstance, slackChannel, authorName, color, title, message, buildUrl, buildResult, fallbackText = "Full message not displayed") {
        JSONArray attachments = new JSONArray()
        JSONObject attachment = new JSONObject()
        JSONArray markdownSupport = new JSONArray()
        markdownSupport.add('text')
        markdownSupport.add('pretext')
        attachment.put('mrkdwn_in', markdownSupport)

        attachment.put('author_name', authorName)
        attachment.put('color', color)
        attachment.put('title', title.toString())
        attachment.put('title_link', buildUrl)
        attachment.put('text', message.toString())
        attachment.put('fallback', fallbackText)

        JSONArray actions = new JSONArray()
        JSONObject action = new JSONObject()
        action.put('type', 'button')
        action.put('text', 'View Build Details')
        action.put('style', 'primary')
        action.put('url', buildUrl)
        actions.add(action)
        attachment.put('actions', actions)

        if (buildResult == 'FAILURE' || buildResult == 'UNSTABLE') {
            JSONArray fields = new JSONArray()
            JSONObject field = new JSONObject()
            field.put('title', 'Priority')
            field.put('value', 'High')
            fields.add(field)
            attachment.put('fields', fields)
        }

        attachments.add(attachment)
        scriptInstance.slackSend (channel: slackChannel, color: color, attachments: attachments.toString())
    }
}
