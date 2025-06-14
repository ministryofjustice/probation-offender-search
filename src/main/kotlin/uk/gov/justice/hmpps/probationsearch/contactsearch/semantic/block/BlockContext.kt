package uk.gov.justice.hmpps.probationsearch.contactsearch.semantic.block

class BlockContext {
  internal var action: () -> Unit = {}
  internal var rollback: () -> Unit = {}
  fun action(block: () -> Unit) {
    action = block
  }

  fun rollback(block: () -> Unit) {
    rollback = block
  }
}
