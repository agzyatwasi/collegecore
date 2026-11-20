package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.signOutUser
import com.example.data.CollegeRepository
import com.example.model.UiState
import com.example.ui.CollegeViewModel
import com.example.ui.components.LoadingStateView
import com.example.ui.screens.*
import com.example.ui.theme.CollegeCoreTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object Routes {
  const val HOME = "home"
  const val ACADEMICS = "academics/{tab}"
  const val NOTICES = "notices"
  const val GALLERY = "gallery"
  const val PROFILE = "profile"
  const val NOTICE_DETAIL = "notice_detail/{noticeId}"
  const val ALBUM_DETAIL = "album_detail/{albumId}"
  const val ADMIN = "admin"

  fun academics(tab: Int = 0) = "academics/$tab"
  fun noticeDetail(id: String) = "notice_detail/$id"
  fun albumDetail(id: String) = "album_detail/$id"
}

private data class NavTabItem(
  val route: String,
  val baseRoute: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CollegeCoreTheme(darkTheme = false) {
        AppContent()
      }
    }
  }
}

@Composable
fun AppContent() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var requestedAdminPortal by remember { mutableStateOf(false) }
  var authGateError by remember { mutableStateOf<String?>(null) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      externalError = authGateError,
      onClearExternalError = { authGateError = null },
      onSignInCompleted = { isAdminRequested ->
        requestedAdminPortal = isAdminRequested
        currentUser = Firebase.auth.currentUser
      },
    )
  } else {
    val uid = user.uid
    key(uid) {
      val collegeViewModel: CollegeViewModel =
        viewModel(
          key = uid,
          factory = viewModelFactory {
            initializer {
              val app = checkNotNull(this[APPLICATION_KEY])
              val databaseId = app.getString(R.string.firestore_database_id)
              val db = FirebaseFirestore.getInstance(databaseId)
              CollegeViewModel(CollegeRepository(db), uid)
            }
          },
        )

      // Enforce Admin Portal check if user tapped "Admin Sign-In with Google"
      var adminVerificationPending by remember(uid, requestedAdminPortal) {
        mutableStateOf(requestedAdminPortal)
      }
      LaunchedEffect(uid, requestedAdminPortal) {
        if (requestedAdminPortal) {
          val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
          val repo = CollegeRepository(FirebaseFirestore.getInstance(databaseId))
          val allowed = repo.checkIsAdminOnce(uid)
          if (!allowed) {
            authGateError =
              "Access denied: ${user.email ?: "This account"} is not an authorized ITG Gopeshwar administrator."
            requestedAdminPortal = false
            signOutUser(credentialManager, scope)
          } else {
            adminVerificationPending = false
          }
        }
      }

      if (adminVerificationPending) {
        Surface(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
          LoadingStateView("Verifying administrator authorization...")
        }
      } else {
        CollegeCoreAuthenticatedShell(
          viewModel = collegeViewModel,
          startInAdmin = requestedAdminPortal,
          onConsumedAdminStart = { requestedAdminPortal = false },
          onSignOut = {
            requestedAdminPortal = false
            signOutUser(credentialManager, scope)
          },
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollegeCoreAuthenticatedShell(
  viewModel: CollegeViewModel,
  startInAdmin: Boolean,
  onConsumedAdminStart: () -> Unit,
  onSignOut: () -> Unit,
) {
  val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
  val profileState by viewModel.studentProfileState.collectAsStateWithLifecycle()
  val coursesState by viewModel.coursesState.collectAsStateWithLifecycle()
  val branchesState by viewModel.branchesState.collectAsStateWithLifecycle()
  val subjectsState by viewModel.subjectsState.collectAsStateWithLifecycle()
  val examCatsState by viewModel.examCategoriesState.collectAsStateWithLifecycle()
  val noticesState by viewModel.noticesState.collectAsStateWithLifecycle()
  val readNoticeIds by viewModel.readNoticeIds.collectAsStateWithLifecycle()
  val syllabusState by viewModel.syllabusState.collectAsStateWithLifecycle()
  val materialsState by viewModel.studyMaterialsState.collectAsStateWithLifecycle()
  val papersState by viewModel.examPapersState.collectAsStateWithLifecycle()
  val albumsState by viewModel.galleryAlbumsState.collectAsStateWithLifecycle()
  val albumPhotosState by viewModel.albumPhotosState.collectAsStateWithLifecycle()
  val adminStudentsState by viewModel.adminStudentsState.collectAsStateWithLifecycle()
  val adminsListState by viewModel.adminsListState.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
  val feedback by viewModel.feedback.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(feedback) {
    val item = feedback ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(item.message)
    viewModel.clearFeedback()
  }

  val courses = (coursesState as? UiState.Success)?.data.orEmpty()
  val branches = (branchesState as? UiState.Success)?.data.orEmpty()
  val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
  val examCategories = (examCatsState as? UiState.Success)?.data.orEmpty()
  val notices = (noticesState as? UiState.Success)?.data.orEmpty()
  val syllabusList = (syllabusState as? UiState.Success)?.data.orEmpty()
  val materials = (materialsState as? UiState.Success)?.data.orEmpty()
  val papers = (papersState as? UiState.Success)?.data.orEmpty()
  val albums = (albumsState as? UiState.Success)?.data.orEmpty()
  val students = (adminStudentsState as? UiState.Success)?.data.orEmpty()
  val admins = (adminsListState as? UiState.Success)?.data.orEmpty()

  var isEditingProfile by remember { mutableStateOf(false) }
  var showAdminScreen by remember(startInAdmin) { mutableStateOf(startInAdmin) }

  LaunchedEffect(startInAdmin) {
    if (startInAdmin) onConsumedAdminStart()
  }

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route

  if (showAdminScreen && isAdmin) {
    BackHandler { showAdminScreen = false }
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
      Box(Modifier.fillMaxSize().padding(innerPadding)) {
        AdminDashboardScreen(
          viewModel = viewModel,
          students = students,
          admins = admins,
          courses = courses,
          branches = branches,
          subjects = subjects,
          examCategories = examCategories,
          notices = notices,
          syllabusList = syllabusList,
          studyMaterials = materials,
          examPapers = papers,
          galleryAlbums = albums,
          isBusy = isBusy,
          onBack = { showAdminScreen = false },
        )
      }
    }
    return
  }

  when (val profState = profileState) {
    is UiState.Loading -> {
      Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
          LoadingStateView("Loading your ITG Gopeshwar profile...")
        }
      }
    }

    is UiState.Error -> {
      Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = profState.message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
          )
          Spacer(Modifier.height(12.dp))
          Button(onClick = onSignOut) { Text("Sign Out") }
        }
      }
    }

    is UiState.Success -> {
      val profile = profState.data
      if (profile == null || isEditingProfile) {
        if (isEditingProfile) {
          BackHandler { isEditingProfile = false }
        }
        Scaffold(
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            TopAppBar(
              title = { Text("ITG Gopeshwar • College Core") },
            )
          },
        ) { innerPadding ->
          Box(Modifier.fillMaxSize().padding(innerPadding)) {
            StudentProfileSetupScreen(
              existingProfile = profile,
              courses = courses,
              branches = branches,
              isBusy = isBusy,
              isAdmin = isAdmin,
              onSaveProfile = { name, roll, course, branch, year, sem, photo ->
                viewModel.saveStudentProfile(
                  fullName = name,
                  rollNumber = roll,
                  course = course,
                  branch = branch,
                  academicYear = year,
                  semester = sem,
                  photoUrl = photo,
                  isExistingProfile = profile != null,
                ) {
                  isEditingProfile = false
                }
              },
              onSeedDemoData = { viewModel.seedDemoData() },
              onOpenAdminDashboard = { showAdminScreen = true },
              onSignOut = onSignOut,
              onCancelEdit = if (profile != null) { { isEditingProfile = false } } else null,
            )
          }
        }
        return
      }

      val navItems = remember {
        listOf(
          NavTabItem(Routes.HOME, Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
          NavTabItem(Routes.academics(0), "academics", "Academics", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_academics"),
          NavTabItem(Routes.NOTICES, Routes.NOTICES, "Notices", Icons.Filled.Campaign, Icons.Outlined.Campaign, "nav_notices"),
          NavTabItem(Routes.GALLERY, Routes.GALLERY, "Gallery", Icons.Filled.PhotoLibrary, Icons.Outlined.PhotoLibrary, "nav_gallery"),
          NavTabItem(Routes.PROFILE, Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile"),
        )
      }

      val showTopLevelChrome =
        currentRoute == Routes.HOME ||
          currentRoute?.startsWith("academics") == true ||
          currentRoute == Routes.NOTICES ||
          currentRoute == Routes.GALLERY ||
          currentRoute == Routes.PROFILE

      val unreadBadgeCount =
        notices.count {
          (isAdmin || it.published) && it.matchesStudent(profile) && it.id !in readNoticeIds
        }

      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
          if (useRail && showTopLevelChrome) {
            NavigationRail(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
              Spacer(Modifier.height(12.dp))
              navItems.forEach { item ->
                val selected =
                  currentRoute == item.baseRoute ||
                    (item.baseRoute == "academics" && currentRoute.startsWith("academics"))
                NavigationRailItem(
                  selected = selected,
                  onClick = {
                    navController.navigate(item.route) {
                      popUpTo(Routes.HOME) { saveState = true }
                      launchSingleTop = true
                      restoreState = true
                    }
                  },
                  icon = {
                    BadgedBox(
                      badge = {
                        if (item.baseRoute == Routes.NOTICES && unreadBadgeCount > 0) {
                          Badge { Text(unreadBadgeCount.toString()) }
                        }
                      }
                    ) {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                      )
                    }
                  },
                  label = { Text(item.label) },
                  modifier = Modifier.testTag(item.testTag),
                )
              }
            }
          }

          Scaffold(
            modifier = Modifier.weight(1f),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
              if (showTopLevelChrome) {
                TopAppBar(
                  title = {
                    val title =
                      when {
                        currentRoute == Routes.NOTICES -> "Notices & Updates"
                        currentRoute.startsWith("academics") -> "Academics"
                        currentRoute == Routes.GALLERY -> "Campus Gallery"
                        currentRoute == Routes.PROFILE -> "My Profile"
                        else -> "College Core"
                      }
                    Text(title)
                  },
                  actions = {
                    if (isAdmin) {
                      TextButton(
                        onClick = { showAdminScreen = true },
                        modifier = Modifier.testTag("topbar_admin_button"),
                      ) {
                        Icon(
                          Icons.Filled.AdminPanelSettings,
                          contentDescription = null,
                          modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Admin")
                      }
                    }
                  },
                )
              }
            },
            bottomBar = {
              if (!useRail && showTopLevelChrome) {
                NavigationBar {
                  navItems.forEach { item ->
                    val selected =
                      currentRoute == item.baseRoute ||
                        (item.baseRoute == "academics" && currentRoute.startsWith("academics"))
                    NavigationBarItem(
                      selected = selected,
                      onClick = {
                        navController.navigate(item.route) {
                          popUpTo(Routes.HOME) { saveState = true }
                          launchSingleTop = true
                          restoreState = true
                        }
                      },
                      icon = {
                        BadgedBox(
                          badge = {
                            if (item.baseRoute == Routes.NOTICES && unreadBadgeCount > 0) {
                              Badge { Text(unreadBadgeCount.toString()) }
                            }
                          }
                        ) {
                          Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                          )
                        }
                      },
                      label = { Text(item.label) },
                      modifier = Modifier.testTag(item.testTag),
                    )
                  }
                }
              }
            },
          ) { innerPadding ->
            NavHost(
              navController = navController,
              startDestination = Routes.HOME,
              modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
              composable(Routes.HOME) {
                StudentDashboardScreen(
                  profile = profile,
                  isAdmin = isAdmin,
                  notices = notices,
                  readNoticeIds = readNoticeIds,
                  studyMaterials = materials,
                  onNavigateNotices = { navController.navigate(Routes.NOTICES) },
                  onNavigateAcademicsTab = { tab -> navController.navigate(Routes.academics(tab)) },
                  onNavigateGallery = { navController.navigate(Routes.GALLERY) },
                  onNavigateProfile = { navController.navigate(Routes.PROFILE) },
                  onOpenNoticeDetail = { notice ->
                    navController.navigate(Routes.noticeDetail(notice.id))
                  },
                  onOpenAdminDashboard = { showAdminScreen = true },
                  onSeedDemoData = { viewModel.seedDemoData() },
                  isBusy = isBusy,
                )
              }

              composable(
                route = Routes.ACADEMICS,
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 }),
              ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                AcademicsScreen(
                  initialTab = tab,
                  profile = profile,
                  isAdmin = isAdmin,
                  courses = courses,
                  branches = branches,
                  examCategories = examCategories,
                  syllabusList = syllabusList,
                  studyMaterials = materials,
                  examPapers = papers,
                )
              }

              composable(Routes.NOTICES) {
                NoticesScreen(
                  noticesState = noticesState,
                  profile = profile,
                  isAdmin = isAdmin,
                  readNoticeIds = readNoticeIds,
                  onOpenNotice = { notice ->
                    navController.navigate(Routes.noticeDetail(notice.id))
                  },
                )
              }

              composable(
                route = Routes.NOTICE_DETAIL,
                arguments = listOf(navArgument("noticeId") { type = NavType.StringType }),
              ) { backStackEntry ->
                val noticeId = backStackEntry.arguments?.getString("noticeId").orEmpty()
                val notice = notices.firstOrNull { it.id == noticeId }
                NoticeDetailScreen(
                  notice = notice,
                  isRead = noticeId in readNoticeIds,
                  onMarkRead = { viewModel.markNoticeAsRead(it) },
                  onBack = { navController.popBackStack() },
                )
              }

              composable(Routes.GALLERY) {
                GalleryScreen(
                  albumsState = albumsState,
                  isAdmin = isAdmin,
                  onOpenAlbum = { album ->
                    viewModel.selectAlbum(album.id)
                    navController.navigate(Routes.albumDetail(album.id))
                  },
                )
              }

              composable(
                route = Routes.ALBUM_DETAIL,
                arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
              ) { backStackEntry ->
                val albumId = backStackEntry.arguments?.getString("albumId").orEmpty()
                LaunchedEffect(albumId) { viewModel.selectAlbum(albumId) }
                val album = albums.firstOrNull { it.id == albumId }
                AlbumDetailScreen(
                  album = album,
                  photosState = albumPhotosState,
                  isAdmin = isAdmin,
                  onBack = { navController.popBackStack() },
                )
              }

              composable(Routes.PROFILE) {
                ProfileScreen(
                  profile = profile,
                  isAdmin = isAdmin,
                  onEditProfile = { isEditingProfile = true },
                  onOpenAdminDashboard = { showAdminScreen = true },
                  onSignOut = onSignOut,
                )
              }
            }
          }
        }
      }
    }
  }
}
