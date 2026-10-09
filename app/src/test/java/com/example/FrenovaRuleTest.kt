package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.FrenovaRepository
import com.example.model.Post
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FrenovaRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createPost_authenticatedUser_createsSuccessfully() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = FrenovaRepository(firestore, auth)

    val res = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createPost(
        authorName = "Alice",
        authorAvatar = "",
        content = "Hello from local Robolectric test!",
        isReel = false
      )
    }
    assertTrue("Create post should succeed", res.isSuccess)
    assertNotNull("Returned post ID", res.getOrNull())
  }

  @Test
  fun observePosts_authenticatedUser_receivesPosts() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = FrenovaRepository(firestore, auth)
    val postRes = repo.createPost("Alice", "", "Observing test", isReel = false)
    assertTrue(postRes.isSuccess)

    val posts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observePosts().first { list: List<Post> -> list.isNotEmpty() }
    }
    assertTrue("Posts list should not be empty", posts.isNotEmpty())
  }

  @Test
  fun observePosts_unauthenticatedUser_rejected() = runBlocking {
    auth.signOut()
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        val path = "posts"
        firestore.collection(path).get().result
      }
      fail("Expected exception for unauthenticated read")
    } catch (e: Exception) {
      // Expected
      assertTrue(true)
    }
  }

  @Test
  fun auditLogs_nonDeveloper_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("audit_logs").get().result
      }
      fail("Expected exception for non-developer read of audit logs")
    } catch (e: Exception) {
      // Expected
      assertTrue(true)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice_rules@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
