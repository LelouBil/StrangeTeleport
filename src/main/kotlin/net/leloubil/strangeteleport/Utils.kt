package net.leloubil.strangeteleport

import kotlin.reflect.KFunction



fun <P1,P2,R> ((P1,P2) -> R).flip(): (P2,P1) -> R {
    return { p2,p1 -> this(p1,p2) }
}

fun <P1,P2,P3,R> ((P1,P2,P3) -> R).flip(): (P3,P2,P1) -> R {
    return { p3,p2,p1 -> this(p1,p2,p3) }
}

fun <P1,P2,P3,P4,R> ((P1,P2,P3,P4) -> R).flip(): (P4,P3,P2,P1) -> R {
    return { p4,p3,p2,p1 -> this(p1,p2,p3,p4) }
}

fun <P1,P2,P3,P4,P5,R> ((P1,P2,P3,P4,P5) -> R).flip(): (P5,P4,P3,P2,P1) -> R {
    return { p5,p4,p3,p2,p1 -> this(p1,p2,p3,p4,p5) }
}

fun <P1,P2,P3,P4,P5,P6,R> ((P1,P2,P3,P4,P5,P6) -> R).flip(): (P6,P5,P4,P3,P2,P1) -> R {
    return { p6,p5,p4,p3,p2,p1 -> this(p1,p2,p3,p4,p5,p6) }
}
