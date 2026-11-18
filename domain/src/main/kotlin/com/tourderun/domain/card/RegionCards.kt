package com.tourderun.domain.card

/** 지역 카드 랜드마크 하나 (EXT-01, S18). */
data class Landmark(val name: String, val description: String)

/**
 * 지역 카드 글 콘텐츠 (M-07). 도시 이름(한글)·좌표·순서는 경로 데이터(`Season1Route`)를 쓰고,
 * 여기에는 카드에만 필요한 글만 둔다. 대표 사진은 이용 조건을 확인한 뒤 앱의 drawable로 넣는다.
 *
 * @property photoCredit 사진 출처 표기. 사진이 정해지기 전에는 null(화면은 출처 줄을 숨긴다)
 */
data class RegionCardContent(
    val cityId: String,
    val nameEn: String,
    val intro: String,
    val landmarks: List<Landmark>,
    val photoCredit: String? = null,
)

/**
 * 시즌 1 경로 8개 도시의 카드 콘텐츠. 순서는 경로 순서(서울 출발 → 부산)와 같다.
 *
 * 소개는 2~3문장, 랜드마크는 2~3곳(기능 명세 11장 EXT-01).
 * TODO(EXT-01): 지자체·관광 공식 자료로 사실을 한 번 더 확인하고, 사진과 출처를 채운다.
 */
object RegionCards {
    val all: List<RegionCardContent> = listOf(
        RegionCardContent(
            cityId = "seoul",
            nameEn = "Seoul",
            intro = "조선의 도읍이었고 지금은 대한민국의 수도인 도시예요. " +
                "한강을 따라 강변 러닝 코스가 길게 이어져 있어 여정을 시작하기 좋아요.",
            landmarks = listOf(
                Landmark("경복궁", "1395년에 지어진 조선의 첫 번째 법궁이에요. 광화문을 지나 근정전까지 걸어 볼 수 있어요."),
                Landmark("N서울타워", "남산 꼭대기에 있는 전망 타워예요. 서울 시내를 한눈에 내려다볼 수 있어요."),
                Landmark("한강공원", "한강 양쪽에 이어진 시민 공원이에요. 러너들이 가장 많이 찾는 코스 중 하나예요."),
            ),
        ),
        RegionCardContent(
            cityId = "anyang",
            nameEn = "Anyang",
            intro = "서울 남쪽에 맞닿은 경기도의 도시예요. " +
                "도심을 가로지르는 안양천 산책로를 따라 한강까지 달려갈 수도 있어요.",
            landmarks = listOf(
                Landmark("안양예술공원", "계곡을 따라 여러 작가의 공공 예술 작품이 놓여 있는 공원이에요."),
                Landmark("안양천", "안양을 지나 한강으로 흘러드는 하천이에요. 둔치에 자전거길과 산책로가 있어요."),
            ),
        ),
        RegionCardContent(
            cityId = "osan",
            nameEn = "Osan",
            intro = "경기도 남부의 작은 도시로, 숲과 하천이 가까이 있어요. " +
                "임진왜란 때의 이야기가 남아 있는 산성이 있어요.",
            landmarks = listOf(
                Landmark("물향기수목원", "물을 주제로 꾸민 경기도립 수목원이에요. 습지와 온실을 따라 천천히 걷기 좋아요."),
                Landmark("독산성과 세마대", "임진왜란 때 권율 장군이 왜군을 막아 낸 곳으로 전해지는 산성이에요."),
            ),
        ),
        RegionCardContent(
            cityId = "cheonan",
            nameEn = "Cheonan",
            intro = "충청남도 북부의 교통 중심 도시예요. " +
                "호두과자의 고장으로 잘 알려져 있고, 독립운동의 역사를 기리는 곳이 많아요.",
            landmarks = listOf(
                Landmark("독립기념관", "한국 독립운동의 역사를 전시한 국립 기념관이에요. 넓은 야외 공간도 함께 있어요."),
                Landmark("각원사", "태조산 자락의 절로, 큰 청동 좌불상이 있어요."),
                Landmark("유관순 열사 사적지", "3·1 운동 때 아우내 장터 만세 운동을 이끈 유관순 열사를 기리는 곳이에요."),
            ),
        ),
        RegionCardContent(
            cityId = "sejong",
            nameEn = "Sejong",
            intro = "행정 기능을 나누기 위해 새로 만든 특별자치시예요. " +
                "넓은 호수공원과 정부청사 옥상정원처럼 걷고 달리기 좋은 길이 많아요.",
            landmarks = listOf(
                Landmark("세종호수공원", "도심 한가운데의 큰 인공 호수 공원이에요. 호수를 한 바퀴 도는 산책로가 있어요."),
                Landmark("정부세종청사 옥상정원", "여러 청사 건물의 옥상을 길게 이은 정원이에요."),
                Landmark("국립세종도서관", "책을 펼친 모양의 지붕이 눈에 띄는 국립 도서관이에요."),
            ),
        ),
        RegionCardContent(
            cityId = "yeongdong",
            nameEn = "Yeongdong",
            intro = "충청북도 남쪽 끝, 경상도·전라도와 맞닿은 군이에요. " +
                "포도와 와인으로 이름났고, 조선의 음악가 난계 박연의 고향이에요.",
            landmarks = listOf(
                Landmark("월류봉", "달이 머물다 간다는 이름의 봉우리예요. 아래로 초강천이 휘감아 흘러요."),
                Landmark("난계국악박물관", "박연과 우리 전통 악기를 소개하는 박물관이에요."),
                Landmark("노근리평화공원", "한국전쟁 때의 노근리 사건을 기억하고 평화를 기리는 공원이에요."),
            ),
        ),
        RegionCardContent(
            cityId = "daegu",
            nameEn = "Daegu",
            intro = "경상도 내륙의 큰 도시로, 분지라서 여름이 덥기로 유명해요. " +
                "오래된 시장과 골목길에 이야기가 많이 남아 있어요.",
            landmarks = listOf(
                Landmark("팔공산", "대구 북쪽을 감싸는 큰 산이에요. 갓바위 부처로 유명해요."),
                Landmark("서문시장", "오래된 전통 시장으로, 밤에는 야시장이 열려요."),
                Landmark("김광석 다시 그리기 길", "가수 김광석을 기리는 벽화 골목이에요."),
            ),
        ),
        RegionCardContent(
            cityId = "busan",
            nameEn = "Busan",
            intro = "대한민국 제2의 도시이자 가장 큰 항구 도시예요. " +
                "서울에서 350km를 달려온 여정이 끝나는 곳이에요. 완주를 축하해요!",
            landmarks = listOf(
                Landmark("해운대 해수욕장", "부산을 대표하는 해변이에요. 백사장을 따라 달리는 러너가 많아요."),
                Landmark("광안대교", "광안리 바다를 가로지르는 다리예요. 밤이면 조명이 켜져요."),
                Landmark("감천문화마을", "산비탈에 알록달록한 집들이 계단처럼 모여 있는 마을이에요."),
            ),
        ),
    )

    /** [cityId]의 카드 콘텐츠. 없는 도시면 null. */
    fun of(cityId: String): RegionCardContent? = all.firstOrNull { it.cityId == cityId }
}
