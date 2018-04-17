package tech.cryptonomic.conseil.tezos

import slick.jdbc.PostgresProfile.api._
import tech.cryptonomic.conseil.tezos.TezosTypes.{AccountsWithBlockHash, Block}

import scala.concurrent.Future

/**
  * Functions for writing Tezos data to a database.
  */
object TezosDatabaseOperations {

  /**
    * Writes blocks and operations to a database.
    * @param blocks   Block with operations.
    * @param dbHandle Handle to database.
    * @return         Future on database inserts.
    */
  def writeBlocksToDatabase(blocks: List[Block], dbHandle: Database): Future[Unit] =
    dbHandle.run(
      DBIO.seq(
        Tables.Blocks                 ++= blocks.map(blockToDatabaseRow),
        Tables.OperationGroups        ++= blocks.flatMap(operationGroupToDatabaseRow)
      )
    )

  /**
    * Writes accounts from a specific blocks to a database.
    * @param accountsInfo Accounts with their corresponding block hash.
    * @param dbHandle     Handle to a database.
    * @return             Future on database inserts.
    */
  def writeAccountsToDatabase(accountsInfo: AccountsWithBlockHash, dbHandle: Database): Future[Unit] =
    dbHandle.run(
      DBIO.seq(
        Tables.Accounts               ++= accountsToDatabaseRows(accountsInfo)
      )
    )

  /**
    * Generates database rows for accounts.
    * @param accountsInfo Accounts
    * @return             Database rows
    */
  def accountsToDatabaseRows(accountsInfo: AccountsWithBlockHash): List[Tables.AccountsRow] =
    accountsInfo.accounts.map { account =>
      Tables.AccountsRow(
        account._1,
        accountsInfo.block_hash,
        account._2.manager,
        account._2.spendable,
        account._2.delegate.setable,
        account._2.delegate.value,
        account._2.counter,
        Some(account._2.script)
      )
    }.toList

  /**
    * Generates database rows for blocks.
    * @param block  Block
    * @return       Database rows
    */
  def blockToDatabaseRow(block: Block): Tables.BlocksRow =
    Tables.BlocksRow(
      block.metadata.chain_id,
      block.metadata.protocol,
      block.metadata.level,
      block.metadata.proto,
      block.metadata.predecessor,
      block.metadata.validation_pass,
      block.metadata.operations_hash,
      block.metadata.protocol_data,
      block.metadata.hash,
      block.metadata.timestamp,
      block.metadata.fitness.mkString(",")
    )

  /**
    * Generates database rows for a block's operation groups.
    * @param block  Block
    * @return       Database rows
    */
  def operationGroupToDatabaseRow(block: Block): List[Tables.OperationGroupsRow] =
    block.operationGroups.map{ og =>
      Tables.OperationGroupsRow(
        og.hash,
        og.branch,
        og.kind,
        og.block,
        og.level,
        fixSlots(og.slots),
        og.signature,
        og.proposals,
        og.period,
        og.source,
        og.proposal,
        og.ballot,
        og.chain,
        og.counter,
        og.fee
      )
    }

  private def fixSlots(slots: Option[List[Int]]): Option[String] =
    slots.flatMap{s: Seq[Int] => Some(s.mkString(","))}
}
