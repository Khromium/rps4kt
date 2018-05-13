package rps

import javafx.util.Pair
import java.util.*

/**
 * battle field is using java-reflection
 * https://github.com/Khromium/battlefield
 *
 * Therefore, I ported "multi_armed_bandit.py".
 */
class Engine(var abst: AbstractAgent = UCBGoalDiffAgent()) : RPSListener {//To select Algorithm, change the constructor.
    /**
     * Set team name
     * "チーム" is added after team name
     */
    override fun setTeamName(): String {
        return "こくさい"
    }

    override fun sendRPS(): Pair<RPS, RPS> {
        return abst.selectArm()
    }

    /**
     * This is a callback to return the result
     * @param victory How much we victories
     * @param own     What we choosed
     * @param enemy   What the enemy choosed
     */
    override fun onResult(victory: Int, own: Pair<RPS, RPS>, enemy: Pair<RPS, RPS>) {
        abst.onResult(victory, own, enemy)
    }


    /**
     * ε-greedy
     */
    class EpsilonGreedy : AbstractAgent() {

        val epsilon = 0.4
        override fun estimate(): Pair<RPS, RPS> {
            var max = armHistory.withIndex().maxBy { (index, value) -> value.sum().toDouble() / value.size }!!.index
            return index2RPS(max)//0番目と1番目に自分の手が入っている
        }

        override fun selectArm(): Pair<RPS, RPS> {
//            println("EPS:${playNum()}")
            armHistory.forEachIndexed { index, mutableList -> if (mutableList.size == 0) return index2RPS(index) }//Check all hands are selected
            return if (Random().nextDouble() <= epsilon) randomRPS() else estimate()
        }

    }

    /**
     * ε-greedy
     * update epsiron
     */
    class EpsilonGreedyGrad : AbstractAgent() {

        var epsilon = 0.7
        override fun estimate(): Pair<RPS, RPS> {
            var max = armHistory.withIndex().maxBy { (index, value) -> value.sum().toDouble() / value.size }!!.index
            if (epsilon > 0) {
                epsilon -= epsilon / 50000.0
            }
            return index2RPS(max)//0番目と1番目に自分の手が入っている
        }

        override fun selectArm(): Pair<RPS, RPS> {
//            println("EPS:${playNum()}")
            armHistory.forEachIndexed { index, mutableList -> if (mutableList.size == 0) return index2RPS(index) }//Check all hands are selected
            return if (Random().nextDouble() <= epsilon) randomRPS() else estimate()
        }

    }


    /**
     * UCB
     */
    class UCBAgent : AbstractAgent() {
        override fun selectArm(): Pair<RPS, RPS> {
//            println("UCB:${playNum()}")
            armHistory.forEachIndexed { index, mutableList -> if (mutableList.size == 0) return index2RPS(index) }//Check all hands are selected
            return estimate()
        }

        override fun estimate(): Pair<RPS, RPS> {
            var max = armHistory.withIndex().maxBy { (index, value) ->
                value.sum().toDouble() / value.size + 2 * Math.sqrt(2 * Math.log(2 * playNum().toDouble() / value.size))
            }!!.index
            return index2RPS(max)
        }

    }

    /**
     * greedy
     */
    class Greedy : AbstractAgent() {
        val firstround = 100
        override fun selectArm(): Pair<RPS, RPS> {
//            println("GRD:${playNum()}")
            armHistory.forEachIndexed { index, mutableList -> if (mutableList.size < firstround) return index2RPS(index) }//Check all hands are selected
            return estimate()
        }

        override fun estimate(): Pair<RPS, RPS> {
            var max = armHistory.withIndex().maxBy { (index, value) -> value.sum().toDouble() / value.size }!!.index
            return index2RPS(max)
        }

    }

    /**
     * UCB-Goal difference
     * Use the goal difference as a reward.
     */
    class UCBGoalDiffAgent : AbstractAgent() {
        override fun selectArm(): Pair<RPS, RPS> {
//            println("UCB:${playNum()}")
            armGoalDiffHistory.forEachIndexed { index, mutableList -> if (mutableList.size == 0) return index2RPS(index) }//Check all hands are selected
            return estimate()
        }

        override fun estimate(): Pair<RPS, RPS> {
            var max = armGoalDiffHistory.withIndex().maxBy { (index, value) ->
                value.sum().toDouble() / value.size + 4 * Math.sqrt(2 * Math.log(2 * playNum().toDouble() / value.size))
            }!!.index
            return index2RPS(max)
        }

    }

    /**
     * skeleton
     */
    abstract class AbstractAgent {
        companion object {
            var armHistory = Array<MutableList<Int>>(6, { mutableListOf() }) //Arm history
            var armGoalDiffHistory = Array<MutableList<Int>>(6, { mutableListOf() }) //Arm history
            var diffArray = arrayOf(
                    arrayOf(2, 3, 1, 3, 4, 2, 1, 2, 0),
                    arrayOf(1, 2, 2, 2, 3, 2, 2, 2, 2),
                    arrayOf(3, 2, 2, 2, 2, 2, 2, 2, 1),
                    arrayOf(1, 2, 2, 2, 3, 2, 2, 2, 2),
                    arrayOf(0, 1, 2, 1, 2, 3, 2, 3, 4),
                    arrayOf(2, 2, 2, 2, 1, 2, 2, 2, 3),
                    arrayOf(3, 2, 2, 2, 2, 2, 2, 2, 1),
                    arrayOf(2, 2, 2, 2, 1, 2, 2, 2, 3),
                    arrayOf(4, 2, 3, 2, 0, 1, 3, 1, 2))//Goal difference array. This array uses worst goal difference as 0.
            /**
             * example:
             * own         enemy
             * Rock Rock * Rock Rock
             *  score is 2
             *
             * Rock Rock * Rock Scissors
             *  score is 3
             *
             * Rock Rock * Scissors Scissors
             *  score is 4
             */
        }

        protected abstract fun estimate(): Pair<RPS, RPS>
        abstract fun selectArm(): Pair<RPS, RPS>
        fun onResult(victory: Int, own: Pair<RPS, RPS>, enemy: Pair<RPS, RPS>) {
            armHistory[rps2index(own)].add(victory)
            armGoalDiffHistory[rps2index(own)].add(diffArray[own.key.rps + own.value.rps * 3][enemy.key.rps + enemy.value.rps * 3])
        }

        /**
         * Convert RPS to internal index.
         * This is because some combinations are same.
         */
        protected fun rps2index(own: Pair<RPS, RPS>): Int {
            if (own.key.rps == RPS.ROCK && own.value.rps == RPS.ROCK) return 0
            if (own.key.rps == RPS.SCISSOR && own.value.rps == RPS.SCISSOR) return 1
            if (own.key.rps == RPS.PAPER && own.value.rps == RPS.PAPER) return 2
            if (own.key.rps == RPS.PAPER && own.value.rps == RPS.SCISSOR || own.key.rps == RPS.SCISSOR && own.value.rps == RPS.PAPER) return 3
            if (own.key.rps == RPS.SCISSOR && own.value.rps == RPS.ROCK || own.key.rps == RPS.ROCK && own.value.rps == RPS.SCISSOR) return 4
            if (own.key.rps == RPS.PAPER && own.value.rps == RPS.ROCK || own.key.rps == RPS.ROCK && own.value.rps == RPS.PAPER) return 5
            return -1
        }

        /**
         * Convert internal index to RPS
         */
        protected fun index2RPS(index: Int): Pair<RPS, RPS> {
            if (index == 0) return Pair(RPS().setRps(RPS.ROCK), RPS().setRps(RPS.ROCK))
            if (index == 1) return Pair(RPS().setRps(RPS.SCISSOR), RPS().setRps(RPS.SCISSOR))
            if (index == 2) return Pair(RPS().setRps(RPS.PAPER), RPS().setRps(RPS.PAPER))
            if (index == 3) return if (Random().nextInt(2) == 0) { //To scramble enemy
                Pair(RPS().setRps(RPS.PAPER), RPS().setRps(RPS.SCISSOR))
            } else {
                Pair(RPS().setRps(RPS.SCISSOR), RPS().setRps(RPS.PAPER))
            }
            if (index == 4) return if (Random().nextInt(2) == 0) {
                Pair(RPS().setRps(RPS.SCISSOR), RPS().setRps(RPS.ROCK))
            } else {
                Pair(RPS().setRps(RPS.ROCK), RPS().setRps(RPS.SCISSOR))
            }

            if (index == 5) return if (Random().nextInt(2) == 1) {
                Pair(RPS().setRps(RPS.PAPER), RPS().setRps(RPS.ROCK))
            } else {
                Pair(RPS().setRps(RPS.ROCK), RPS().setRps(RPS.PAPER))
            }
            return randomRPS()
        }

        protected fun randomRPS(): Pair<RPS, RPS> {
            return Pair(RPS().setRps(Random().nextInt(3)), RPS().setRps(Random().nextInt(3)))
        }

        protected fun playNum(): Int {
            var count = 0
            armHistory.forEach { count += it.size }
            return count
        }
    }


}
