package com.reset.feature.checkin.classify

import com.reset.model.domain.checkin.NeedState

/**
 * Tier-2's tunable data: 4–6 example phrases per need, embedded once (via whatever
 * [com.reset.model.domain.checkin.TextEmbedder] is bound) and cached to disk as vectors — the
 * phrases themselves live here, in plain Kotlin, so they're readable and unit-testable without
 * touching the embedder or its cache. Each phrase is a short, natural sentence rather than a
 * bare keyword, since a sentence encoder is scored on sentence-shaped input.
 */
internal object NeedPrototypes {
val PHRASES: Map<NeedState, List<String>> = mapOf(
    NeedState.BODY_TENSION to listOf(
        "my neck and shoulders are so tight",
        "I'm stiff and achy all over",
        "I feel knotted up and sore",
        "I have a massive knot in my shoulder blade",
        "my joints feel completely locked up",
        "I need to stretch, my whole body is stiff",
        "my muscles are screaming after sitting so long",
        "I've got a constant dull ache in my upper back",
        "I need to crack my back so badly right now",
        "my back is tense from sitting",
        "my posture has wrecked my shoulders today",
        "my lower back is absolutely killing me",
        "I've been hunched over my desk for hours",
        "I feel like I aged 10 years just sitting at this desk",
        "my spine feels compressed from slouching",
        "sitting like a shrimp all day has ruined my posture",
        "my glutes and hamstrings are numb from this chair",
        "I've got that terrible tech-neck ache that just won't quit",
        "my jaw is aching from clenching my teeth all day",
        "my shoulders are basically up by my ears at this point",
        "I feel like I'm carrying weights on my shoulders",
        "I'm holding all my stress right in my shoulder blades"
    ),

    NeedState.HAND_STRAIN to listOf(
        "my wrists hurt from typing all day",
        "my hands are cramping up",
        "my fingers are sore from the keyboard",
        "carpal tunnel is flaring up again",
        "my forearms ache from the mouse",
        "my wrists are cracking and popping today",
        "my grip feels weak from typing non-stop",
        "I have a shooting pain down my forearm",
        "typing is literally painful at this point",
        "my thumbs are throbbing from scrolling",
        "I've got that classic smartphone pinky ache",
        "even scrolling on my phone hurts my thumb joints",
        "I think my thumb is permanently stuck in a scrolling position",
        "my fingers are starting to go a little numb",
        "I keep having to shake out my hands to get the feeling back",
        "the tendons in my hands feel so inflamed",
        "my mouse hand is completely useless at this point",
        "I desperately need to dunk my hands in a bucket of ice water"
    ),

    NeedState.EYE_STRAIN to listOf(
        "my eyes are so tired from the screen",
        "everything looks blurry after all this screen time",
        "the blue light is giving me a migraine",
        "the monitor is too bright and my eyes hurt",
        "I think my screen brightness is frying my retinas",
        "I have a headache behind my eyes",
        "my eyes feel dry and strained",
        "my eyes are burning and watering",
        "I need some eye drops, my eyes are like sandpaper",
        "I keep rubbing my eyes but it doesn't help",
        "I have a dull ache right between my eyebrows",
        "my eyes are so dry they feel like they're scraping against my eyelids",
        "I keep having to blink to clear my vision",
        "everything on my monitor is starting to double",
        "my vision is getting fuzzy from all this computer work",
        "I feel like I'm staring through a thick layer of fog"
    ),

    NeedState.WOUND_UP to listOf(
        "I'm so stressed and on edge right now",
        "I'm really frustrated and angry",
        "everything is winding me up today",
        "I'm irritable and can't calm down",
        "my patience is wearing incredibly thin right now",
        "I feel like I'm ready to snap at someone",
        "my blood is boiling over the smallest things",
        "I am just one bad email away from losing it",
        "I can't do my job because I'm waiting on approvals",
        "I keep getting blocked by IT policies",
        "this meeting could have been an email and I'm mad about it",
        "everything and everyone is getting on my last nerve",
        "my manager needs an update every five minutes",
        "I have zero autonomy over my own work",
        "I feel like my boss is breathing down my neck",
        "I'm being thrown under the bus",
        "I feel like I'm constantly defending my work",
        "I'm feeling so triggered and defensive today",
        "I need a punching bag, immediately",
        "I'm dangerously close to throwing my phone across the room",
        "I'm vibrating with unexpressed rage right now"
    ),

    NeedState.SCATTERED to listOf(
        "I can't focus on anything today",
        "my mind keeps jumping around",
        "I'm so distracted and unfocused",
        "my thoughts are all over the place",
        "my attention span is absolute zero today",
        "I have zero mental clarity at the moment",
        "I feel foggy and can't concentrate",
        "I keep forgetting what I was just about to do",
        "I have major brain fog and can't string a sentence together",
        "I have 50 tabs open in my brain and they're all frozen",
        "my brain is just buffering endlessly",
        "I haven't had 10 minutes of uninterrupted focus all day",
        "every time I start a task, someone pings me",
        "my brain is fried from context switching",
        "every single notification completely derails my train of thought",
        "I'm bouncing between tasks and finishing nothing",
        "I'm chasing shiny objects all day instead of doing real work"
    ),

    NeedState.DRAINED to listOf(
        "I'm completely exhausted",
        "I have no energy left today",
        "I'm running on empty",
        "I feel so tired and worn out",
        "I'm ready to crawl back into bed right now",
        "I just hit a massive afternoon slump",
        "my body feels like it's made of lead",
        "no amount of coffee is going to fix this level of tired",
        "I'm burnt out and sluggish",
        "I have nothing left to give to this company",
        "I dread waking up and logging on",
        "I am totally running on fumes at this point",
        "I'm mentally and physically tapped out for the week",
        "my life force has been entirely siphoned away by this day",
        "I am spiritually, emotionally, and physically bankrupt",
        "my battery is flashing red and about to shut down"
    ),

    NeedState.LOW_MOOD to listOf(
        "I'm feeling really down today",
        "I feel sad and low",
        "I'm in a bit of a gloomy mood",
        "everything feels heavy right now",
        "there's a dark cloud hanging over my head today",
        "I feel like crying for no specific reason",
        "I just feel kind of empty and hollow today",
        "I feel completely emotionally flatlined",
        "everything feels totally pointless right now",
        "I'm feeling really apathetic about everything",
        "I'm just collecting a paycheck at this point",
        "I literally don't care if this project fails anymore",
        "I'm completely numb to the chaos here",
        "nothing sounds fun, appealing, or worth doing right now",
        "I feel blue and unmotivated",
        "I just want to hide under the covers all day",
        "it takes so much effort just to exist today",
        "I feel like I'm in a rut and can't get out",
        "I'm deep in the trenches of the Sunday Scaries right now"
    ),

    NeedState.STUCK_ON_A_THOUGHT to listOf(
        "I can't stop thinking about this",
        "one thought keeps replaying in my head",
        "I'm ruminating and can't let it go",
        "my mind is stuck in a loop",
        "I keep obsessing over that one conversation",
        "my brain won't let me drop this issue",
        "my brain is acting like a broken record right now",
        "I'm dissecting every word of that email I just sent",
        "I'm overthinking everything right now",
        "I'm spiraling about something that hasn't even happened",
        "I'm going down a massive rabbit hole of 'what-ifs'",
        "I keep playing out worst-case scenarios over and over",
        "my brain is hyper-fixating on a problem that doesn't even exist yet",
        "I feel like everyone else knows what they're doing except me",
        "I'm terrified for this performance review",
        "I hope no one realizes I'm winging this",
        "I'm completely trapped in my own head right now",
        "I'm hyper-fixating on a tiny mistake I made"
    ),

    NeedState.RESTLESS to listOf(
        "I've been sitting too long and need to move",
        "I feel restless and fidgety",
        "I can't sit still right now",
        "I feel like I'm crawling out of my skin",
        "my legs are getting super twitchy",
        "I keep tapping my foot, I have so much nervous energy",
        "I need to burn off some of this weird excess energy",
        "I have way too much energy and I don't know what to do with it",
        "I'm literally bouncing off the walls today",
        "I have the human equivalent of the zoomies",
        "I feel like I just drank five shots of espresso",
        "my brain wants to do cartwheels but my body is stuck in a chair",
        "I'm bored and cooped up",
        "I'm going completely stir-crazy in here",
        "I'm just pretending to look busy",
        "I'm rotting away doing this data entry",
        "I wish I had a project that actually challenged me",
        "I am literally vibrating out of my seat with boredom"
    ),

    NeedState.CANT_WIND_DOWN to listOf(
        "I can't fall asleep, my mind is wired",
        "I'm staring at the ceiling and my brain is racing",
        "I am totally exhausted but my eyes won't stay shut",
        "my body is tired but my mind is doing backflips",
        "my body is practically comatose but my brain is ready to solve world peace",
        "I can't switch off even though it's late",
        "I can't seem to transition out of work mode",
        "I'm lying here wide awake making mental to-do lists",
        "I keep mentally planning my day for tomorrow instead of resting today",
        "I keep scrolling instead of sleeping",
        "I'm suffering from revenge bedtime procrastination",
        "I'm doomscrolling in bed and I know I shouldn't be",
        "I'm stuck in the scroll hole and literally can't put my phone down",
        "I keep checking the time and stressing about how little sleep I'll get",
        "it's 2 AM and I'm calculating exactly how much sleep I can still get",
        "I'm stressed about not sleeping, which is keeping me awake"
    ),

    NeedState.OVERWHELMED to listOf(
        "everything is too much right now",
        "I feel like I'm drowning in everything",
        "I'm completely swamped and don't even know where to start",
        "my plate is way too full right now",
        "I'm juggling way too many balls and they're all about to drop",
        "I have back-to-back meetings all day",
        "my inbox is giving me a panic attack",
        "I can't cope with all of this",
        "I'm at my breaking point",
        "my to-do list is completely paralyzing me",
        "I'm completely and utterly in the weeds right now",
        "I am completely paralyzed by the mountain of things I have to do",
        "I feel like a deer in headlights staring at all these tasks",
        "everything is a priority and it's making my head spin",
        "I feel like I'm constantly just putting out fires",
        "I have major decision fatigue, please don't ask me anything else",
        "I have no bandwidth left for anything else",
        "my brain is short-circuiting from too many inputs"
    ),

    NeedState.DISCONNECTED to listOf(
        "I feel really lonely lately",
        "I feel isolated from everyone",
        "I feel disconnected from people around me",
        "nobody seems to notice me",
        "I feel distant from everyone I know",
        "I'm deeply craving a hug but there's no one around",
        "I feel like I'm working in a vacuum",
        "I miss having watercooler chats",
        "I'm totally out of the loop on what the rest of the team is doing",
        "I haven't talked to anyone outside of work in days",
        "I feel like I'm on a different wavelength from everyone else",
        "I feel like an outsider looking in",
        "no one really gets what I'm going through right now",
        "I feel utterly replaceable and unseen by the world",
        "I feel like a background NPC in someone else's video game",
        "it feels like the whole world is hanging out without me"
    ),

    NeedState.ACCOMPLISHED_FLOW to listOf(
        "put me on DND, I'm crushing this work",
        "I'm in a deep work state right now",
        "I actually just banged out three hours of solid work",
        "I'm hyper-focused and getting so much done",
        "I'm in the zone and don't want to break my streak",
        "my brain is firing on all cylinders right now",
        "I finally cracked that bug",
        "my boss actually shouted me out in the all-hands",
        "I completely crushed that presentation",
        "I got a ton of positive feedback today and I'm riding the high",
        "I feel so competent and on top of my game right now",
        "I actually feel really proud of the work I did today",
        "thank god this week is over",
        "we finally pushed to production",
        "I dodged a massive bullet on that client call",
        "I finally cleared out my entire inbox",
        "a huge weight has been lifted off my shoulders",
        "I crossed the finish line on a massive project"
    ),

    NeedState.BIOLOGICAL_DEPLETION to listOf(
        "I literally forgot to eat lunch today",
        "I'm starving and my blood sugar is crashing",
        "I have a massive caffeine withdrawal headache",
        "I haven't drank a single glass of water all day",
        "I've needed to pee for three hours but I'm stuck on calls",
        "my stomach is growling so loud during this meeting",
        "I'm running purely on iced coffee and zero food",
        "I feel dizzy because I skipped breakfast"
    ),

    NeedState.CONFUSED_LOST to listOf(
        "I have absolutely no idea what I'm doing on this project",
        "I'm completely stuck and don't know what my next step is",
        "the instructions I got make zero sense",
        "I need someone to explain this to me like I'm five",
        "I feel totally lost and don't know who to ask for help",
        "I am staring at this problem and my mind is blank",
        "I'm going in circles trying to figure this out",
        "I really need a second pair of eyes on this"
    ),

    NeedState.TASK_PARALYSIS to listOf(
        "I know exactly what I need to do and I still can't start it",
        "I've been putting this one task off all week",
        "I keep finding other things to do instead of the real thing",
        "I'm dreading starting this so much that I've done nothing for an hour",
        "opening that document feels weirdly impossible right now",
        "I've reorganized my desk twice to avoid this task",
        "I'm frozen every time I look at this thing on my list",
        "it's a twenty minute job and I've avoided it for three days"
    ),

    NeedState.SENSORY_OVERLOAD to listOf(
        "this office is so loud I can't hear myself think",
        "everything is too loud and too bright right now",
        "there's constant chatter and I'm completely overstimulated",
        "my notifications have not stopped pinging all morning",
        "the lights in here are drilling into my skull",
        "there are too many people talking at me at once",
        "every sound is grating on me today",
        "I badly need somewhere dark and quiet for a minute"
    )
)
}