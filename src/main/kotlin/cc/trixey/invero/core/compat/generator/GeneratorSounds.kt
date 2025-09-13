package cc.trixey.invero.core.compat.generator

import cc.trixey.invero.common.sourceObject
import cc.trixey.invero.core.compat.DefGeneratorProvider
import cc.trixey.invero.core.geneartor.BaseGenerator
import taboolib.library.xseries.XSound

/**
 * Invero
 * cc.trixey.invero.core.compat.generator.GeneratorSounds
 *
 * @author Arasple
 * @since 2023/2/2 14:34
 */
@DefGeneratorProvider("sound")
class GeneratorSounds : BaseGenerator() {

    override fun generate() {
        @Suppress("UnstableApiUsage")
        generated = XSound.REGISTRY.mapIndexed { index, sound ->
            sourceObject {
                put("name", sound.name())
                put("ordinal", index)
            }
        }
    }

}