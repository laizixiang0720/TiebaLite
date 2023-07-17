package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.activities.UserActivity
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.thread.PostAgreeBtn
import com.huanchengfly.tieba.post.ui.page.thread.PostCard
import com.huanchengfly.tieba.post.ui.page.thread.UserNameText
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Card
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

@Destination
@Composable
fun SubPostsPage(
    navigator: DestinationsNavigator,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostsId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostsId = subPostsId,
            loadFromSubPost = loadFromSubPost
        )
    }
}

@Destination(
    style = DestinationStyle.BottomSheet::class
)
@Composable
fun SubPostsSheetPage(
    navigator: DestinationsNavigator,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostsId: Long = 0L,
    loadFromSubPost: Boolean = false,
    viewModel: SubPostsViewModel = pageViewModel()
) {
    ProvideNavigator(navigator) {
        SubPostsContent(
            viewModel = viewModel,
            forumId = forumId,
            threadId = threadId,
            postId = postId,
            subPostsId = subPostsId,
            loadFromSubPost = loadFromSubPost
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SubPostsContent(
    viewModel: SubPostsViewModel,
    forumId: Long,
    threadId: Long,
    postId: Long,
    subPostsId: Long = 0L,
    loadFromSubPost: Boolean = false,
) {
    val navigator = LocalNavigator.current

    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(
            SubPostsUiIntent.Load(
                forumId,
                threadId,
                postId,
                subPostId = subPostsId.takeIf { loadFromSubPost } ?: 0L))
    }

    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isRefreshing,
        initial = false
    )
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::isLoading,
        initial = false
    )
    val post by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::post,
        initial = null
    )
    val postContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::postContentRenders,
        initial = persistentListOf()
    )
    val subPosts by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::subPosts,
        initial = persistentListOf()
    )
    val subPostsContentRenders by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::subPostsContentRenders,
        initial = persistentListOf()
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::currentPage,
        initial = 1
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = SubPostsUiState::hasMore,
        initial = false
    )

    val lazyListState = rememberLazyListState()

    viewModel.onEvent<SubPostsUiEvent.ScrollToSubPosts> {
        if (!loadFromSubPost) {
            delay(20)
            lazyListState.scrollToItem(2 + subPosts.indexOfFirst { it.get { id } == subPostsId })
        } else {
            lazyListState.scrollToItem(1)
        }
    }

    StateScreen(
        isEmpty = subPosts.isEmpty(),
        isError = false,
        isLoading = isRefreshing
    ) {
        MyScaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TitleCentredToolbar(
                    title = post?.let {
                        stringResource(
                            id = R.string.title_sub_posts,
                            it.get { floor })
                    } ?: stringResource(id = R.string.title_sub_posts_default),
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(id = R.string.btn_close)
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            LoadMoreLayout(
                isLoading = isLoading,
                loadEnd = !hasMore,
                onLoadMore = {
                    viewModel.send(
                        SubPostsUiIntent.LoadMore(
                            forumId,
                            threadId,
                            postId,
                            currentPage + 1,
                            subPostsId,
                        )
                    )
                }
            ) {
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    state = lazyListState
                ) {
                    item(key = "Post$postId") {
                        post?.let {
                            Column {
                                PostCard(
                                    postHolder = it,
                                    contentRenders = postContentRenders,
                                    showSubPosts = false,
                                    onAgree = {
                                        val hasAgreed = it.get { agree?.hasAgree != 0 }
                                        viewModel.send(
                                            SubPostsUiIntent.Agree(
                                                forumId,
                                                threadId,
                                                postId,
                                                agree = !hasAgreed
                                            )
                                        )
                                    },
                                )
                                VerticalDivider(thickness = 2.dp)
                            }
                        }
                    }
                    stickyHeader(key = "SubPostsHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExtendedTheme.colors.background)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    id = R.string.title_sub_posts_header,
                                    subPosts.size
                                ),
                                style = MaterialTheme.typography.subtitle1
                            )
                        }
                    }
                    itemsIndexed(
                        items = subPosts,
                        key = { _, subPost -> subPost.get { id } }
                    ) { index, item ->
                        SubPostItem(
                            subPost = item,
                            contentRenders = subPostsContentRenders[index],
                            onAgree = {
                                val hasAgreed = it.agree?.hasAgree != 0
                                viewModel.send(
                                    SubPostsUiIntent.Agree(
                                        forumId,
                                        threadId,
                                        postId,
                                        subPostId = it.id,
                                        agree = !hasAgreed
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun getDescText(
    time: Long?,
    ipAddress: String?
): String {
    val texts = mutableListOf<String>()
    if (time != null) texts.add(DateTimeUtils.getRelativeTimeString(App.INSTANCE, time))
    if (!ipAddress.isNullOrEmpty()) texts.add(
        App.INSTANCE.getString(
            R.string.text_ip_location,
            "$ipAddress"
        )
    )
    return texts.joinToString(" ")
}

@Composable
private fun SubPostItem(
    subPost: ImmutableHolder<SubPostList>,
    contentRenders: ImmutableList<PbContentRender>,
    threadAuthorId: Long = 0L,
    onAgree: (SubPostList) -> Unit = {},
) {
    val context = LocalContext.current
    val author = remember(subPost) { subPost.getImmutable { author } }
    val hasAgreed = remember(subPost) {
        subPost.get { agree?.hasAgree == 1 }
    }
    val agreeNum = remember(subPost) {
        subPost.get { agree?.diffAgreeNum ?: 0L }
    }
    Card(
        header = {
            if (author.isNotNull()) {
                author as ImmutableHolder<User>
                UserHeader(
                    avatar = {
                        Avatar(
                            data = StringUtil.getAvatarUrl(author.get { portrait }),
                            size = Sizes.Small,
                            contentDescription = null
                        )
                    },
                    name = {
                        UserNameText(
                            userName = StringUtil.getUsernameAnnotatedString(
                                LocalContext.current,
                                author.get { name },
                                author.get { nameShow }
                            ),
                            userLevel = author.get { level_id },
                            isLz = author.get { id } == threadAuthorId
                        )
                    },
                    desc = {
                        Text(
                            text = getDescText(
                                subPost.get { time }.toLong(),
                                author.get { ip_address })
                        )
                    },
                    onClick = {
                        UserActivity.launch(context, author.get { id }.toString())
                    }
                ) {
                    PostAgreeBtn(
                        hasAgreed = hasAgreed,
                        agreeNum = agreeNum,
                        onClick = { onAgree(subPost.get()) }
                    )
                }
            }
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = Sizes.Small + 8.dp)
            ) {
                contentRenders.forEach { it.Render() }
            }
        }
    )
}