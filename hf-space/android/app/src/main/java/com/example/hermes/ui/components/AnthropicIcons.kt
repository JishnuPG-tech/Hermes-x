package com.example.hermes.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hermes.R
import com.example.hermes.theme.TextPrimaryWarm

/**
 * Reusable icon composables referencing authentic Anthropic line vectors (anthropicon_*).
 */
@Composable
fun AnthropicIcon(
    drawableId: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = TextPrimaryWarm
) {
    Image(
        painter = painterResource(id = drawableId),
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size)
    )
}

object AnthropicIcons {
    val Agent = R.drawable.anthropicon_agent
    val AgentsSimple = R.drawable.anthropicon_agents_simple
    val Dispatch = R.drawable.anthropicon_dispatch
    val Computer = R.drawable.anthropicon_computer
    val Checklist = R.drawable.anthropicon_checklist
    val Create = R.drawable.anthropicon_create
    val Code = R.drawable.anthropicon_code
    val CodeBlock = R.drawable.anthropicon_code_block
    val CommandLine = R.drawable.anthropicon_command_line_prompt
    val Branch = R.drawable.anthropicon_branch
    val Atom = R.drawable.anthropicon_atom
    val Disconnect = R.drawable.anthropicon_disconnect
    val Artifacts = R.drawable.anthropicon_artifacts
    val ArtifactFile = R.drawable.anthropicon_artifact_file
    val Cards = R.drawable.anthropicon_cards
    val BookText = R.drawable.anthropicon_book_text
    val Ghost = R.drawable.anthropicon_ghost
    val Voice = R.drawable.anthropicon_voice
    val Projects = R.drawable.anthropicon_projects
    val Connector = R.drawable.anthropicon_connector
    val Camera = R.drawable.anthropicon_camera
    val Attachment = R.drawable.anthropicon_attachment
    val Spark = R.drawable.claude_spark_icon
    val ExtendedThinking = R.drawable.anthropicon_extended_thinking
    val ArrowLeft = R.drawable.anthropicon_arrow_left
    val ArrowRight = R.drawable.anthropicon_arrow_right
    val CaretDown = R.drawable.anthropicon_caret_down
    val Close = R.drawable.anthropicon_x
    val Check = R.drawable.anthropicon_check
    val CheckCircle = R.drawable.anthropicon_check_circle
    val Copy = R.drawable.anthropicon_copy
    val Microphone = R.drawable.anthropicon_microphone
    val Speaker = R.drawable.anthropicon_speaker
    val Settings = R.drawable.anthropicon_settings
    val Search = R.drawable.anthropicon_search
    val Chat = R.drawable.anthropicon_chat
    val Chats = R.drawable.anthropicon_chats
    val Add = R.drawable.anthropicon_add
    val AddCircle = R.drawable.anthropicon_add_circle
    val Browse = R.drawable.anthropicon_browse
    val Binoculars = R.drawable.anthropicon_binoculars
    val Cloud = R.drawable.anthropicon_cloud
    val Menu = R.drawable.anthropicon_menu
    val Tasks = R.drawable.anthropicon_tasks
    val History = R.drawable.anthropicon_history
    val Knowledge = R.drawable.anthropicon_knowledge
    val Lock = R.drawable.anthropicon_lock
    val Logout = R.drawable.anthropicon_logout
}
