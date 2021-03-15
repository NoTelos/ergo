package org.ergoplatform.nodeView.wallet.persistence

import org.ergoplatform.nodeView.wallet.IdUtils.EncodedBoxId
import org.ergoplatform.utils.WalletTestOps
import org.ergoplatform.utils.generators.WalletGenerators
import org.scalacheck.Gen
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import scala.util.Random


class OffChainRegistrySpec
  extends FlatSpec
    with Matchers
    with GeneratorDrivenPropertyChecks
    with WalletGenerators
    with WalletTestOps {

  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(minSuccessful = 5, sizeRange = 10)

  //registry.updateOnTransaction is called when offchain transaction comes
  it should "calculate indexes correctly on offchain transaction" in {
  forAll(Gen.listOf(trackedBoxGen)) { boxes =>
      //apply transaction outputs to empty offchain registry
      var registry = OffChainRegistry.empty.updateOnTransaction(boxes, Seq.empty)
      val balance = balanceAmount(boxes.map(_.box))
      val assetsBalance = assetAmount(boxes.map(_.box))
      registry.digest.walletBalance shouldEqual balance
      registry.digest.walletAssetBalances shouldEqual assetsBalance

      //spend all the outputs
      registry = registry.updateOnTransaction(Seq.empty, boxes.map(EncodedBoxId @@ _.boxId))
      registry.digest.walletBalance shouldEqual 0
      registry.digest.walletAssetBalances shouldEqual Map.empty
    }
  }

  //registry.updateOnTransaction is called when a block comes
  it should "calculate indexes correctly on a block" in {
    forAll(Gen.listOf(trackedBoxGen)) { boxes =>
      val height = Random.nextInt(500) + 1

      //apply block to empty registry
      val registry = OffChainRegistry.empty.updateOnBlock(height, boxes, boxes.map(EncodedBoxId @@ _.boxId))
      val balance = balanceAmount(boxes.map(_.box))
      val assetsBalance = assetAmount(boxes.map(_.box))
      registry.height shouldEqual height
      registry.digest.walletBalance shouldEqual balance
      registry.digest.walletAssetBalances shouldEqual assetsBalance

      //a block coming is not making any offchain box on-chain
      val registry2 = OffChainRegistry.empty.updateOnBlock(height, boxes, Seq.empty)
      registry2.height shouldEqual height
      registry2.digest.walletBalance shouldEqual balance
      registry2.digest.walletAssetBalances shouldEqual assetsBalance
    }
  }

}
