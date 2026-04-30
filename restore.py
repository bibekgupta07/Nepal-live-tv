import json
import base64
import re

raw_json = """[
    {
        "channel_id": "1413",
        "channel_name": "Kantipur Max 2 HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Kantipur_Max_HD_2024-01-01_05:01:02.png",
        "channel_desc": "Nepali channel",
        "channel_number": "48",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.206/oldlivestream/KTVMax2HD.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTEgQU0maGFzaF92YWx1ZT0vVFc2ZUN5K1ZleStUZUZFZHR1UWRBPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "516",
        "channel_name": "Kantipur HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Kantipur_HD_2021-05-03_10:05:22.png",
        "channel_desc": "Kantipur Television is a privately owned television channel in Nepal.",
        "channel_number": "50",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.206/iptvlivestream/netKANTIPUR1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTEgQU0maGFzaF92YWx1ZT0vVFc2ZUN5K1ZleStUZUZFZHR1UWRBPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "836",
        "channel_name": "AP1 HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/AP1_HD_2021-05-03_10:05:59.png",
        "channel_desc": "Nepali channel",
        "channel_number": "51",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.204/iptvlivedge/netAP1HD1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTEgQU0maGFzaF92YWx1ZT0vVFc2ZUN5K1ZleStUZUZFZHR1UWRBPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "238",
        "channel_name": "Himalaya TV HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Himalaya_TV_HD_2021-05-03_10:05:55.png",
        "channel_desc": "Himalayan Television is  a Nepali News television.",
        "channel_number": "52",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.205/iptvlivestream/netHIMALAYA1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTEgQU0maGFzaF92YWx1ZT0vVFc2ZUN5K1ZleStUZUZFZHR1UWRBPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "225",
        "channel_name": "Nepal TV HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Nepal_TV_HD_2021-05-03_11:05:44.png",
        "channel_desc": "Nepal Television.",
        "channel_number": "53",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netNTVNEPAL1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTEgQU0maGFzaF92YWx1ZT0vVFc2ZUN5K1ZleStUZUZFZHR1UWRBPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "265",
        "channel_name": "NTV News HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/NTV_News_HD_2021-05-03_11:05:04.png",
        "channel_desc": "Popular news channel of Nepal Television",
        "channel_number": "54",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.204/iptvlivestream/netNTVNEWS1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTEgQU0maGFzaF92YWx1ZT0vVFc2ZUN5K1ZleStUZUZFZHR1UWRBPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "262",
        "channel_name": "NTV Plus HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/NTV_Plus_HD_2021-05-03_11:05:27.png",
        "channel_desc": "NTV Plus is another channel from Nepal Television.",
        "channel_number": "55",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.205/iptvlivestream/netNTVPLUS1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "778",
        "channel_name": "Prime Times HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Prime_Times_2020-10-02_06:10:33.png",
        "channel_desc": "Prime TV",
        "channel_number": "56",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.205/iptvlivestream/netPRIMETV1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1312",
        "channel_name": "Makalu TV HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Makalu_TV_HD_2021-01-31_07:01:58.jpg",
        "channel_desc": "Makalu TV HD",
        "channel_number": "57",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netMakaluTvHD.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "429",
        "channel_name": "Janata HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Janata_HD_2021-05-03_11:05:25.png",
        "channel_desc": "Nepali Channel",
        "channel_number": "58",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netJANTATV1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "235",
        "channel_name": "Mountain TV HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Mountain_TV_HD_2022-03-10_10:03:26.png",
        "channel_desc": "Mountain TV is a nepalese news channel .",
        "channel_number": "59",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netMOUNTAIN1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1318",
        "channel_name": "Kantipur Max HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Kantipur_Max_HD_2024-01-01_05:01:02.png",
        "channel_desc": "Nepali Channels Owned by kantipur",
        "channel_number": "61",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netKANTIPURCINEPLEX1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1412",
        "channel_name": "NPL Live",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Kantipur_Max_HD_2024-01-01_05:01:02.png",
        "channel_desc": "NPL Live",
        "channel_number": "61",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.205/npllive/KTVHD4500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1238",
        "channel_name": "METV | HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/METV_2020-11-01_03:11:03.jpg",
        "channel_desc": "local nepali channel",
        "channel_number": "62",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netMeTvHD.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1162",
        "channel_name": "NTV Itahari HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/NTV_Itahari_HD_2020-08-23_08:08:37.png",
        "channel_desc": "Its satellite link channel from Itahari Nepal.",
        "channel_number": "63",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netNTVitahari1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "578",
        "channel_name": "NTV Kohalpur HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/NTV_Kohalpur_HD_2021-05-03_11:05:21.png",
        "channel_desc": "Kohalpur.",
        "channel_number": "64",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.205/iptvlivestream/netNTVKOHALPUR1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1045",
        "channel_name": "Yoho TV HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Yoho_TV_HD_2022-03-10_10:03:53.png",
        "channel_desc": "YOHO TV is nepali channel.",
        "channel_number": "65",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.206/iptvlivestream/netYohoTv1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "422",
        "channel_name": "Business Plus HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Business_Plus_HD_2022-03-10_10:03:20.png",
        "channel_desc": "Bunisness Plus - channel based on business news of Nepal",
        "channel_number": "66",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.204/iptvlivestream/netBusinessplus1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1297",
        "channel_name": "Global TV",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Global_TV_2022-03-10_10:03:47.png",
        "channel_desc": "Nepali",
        "channel_number": "70",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.206/iptvlivestream/netGlobalTvNep.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "237",
        "channel_name": "News 24",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/News_24_2021-05-03_11:05:54.png",
        "channel_desc": "News 24 is a Nepali news channel.",
        "channel_number": "71",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.206/iptvlivestream/netNEWS1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "587",
        "channel_name": "Image HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Image_Channel_2021-09-13_10:09:42.jpg",
        "channel_desc": "The Image Channel.",
        "channel_number": "72",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netIMAGE1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "226",
        "channel_name": "Sagarmatha TV",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Sagarmatha_TV_2021-05-03_11:05:18.png",
        "channel_desc": "Sagarmatha Television.",
        "channel_number": "73",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netSAGAR1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "590",
        "channel_name": "ABC NEWS",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/ABC_NEWS_2021-05-03_11:05:24.png",
        "channel_desc": "ABC Television.",
        "channel_number": "74",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netABC1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "224",
        "channel_name": "Avenues TV",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Avenues_TV_2021-05-03_11:05:44.png",
        "channel_desc": "Avenues Television.",
        "channel_number": "75",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netAVENEWS1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "454",
        "channel_name": "TV Today HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/TV_Today_HD_2021-10-21_09:10:05.png",
        "channel_desc": "TV Today.",
        "channel_number": "76",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.204/iptvlivestream/netTVTODAY1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "288",
        "channel_name": "BTV Business",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/BTV_Business_2022-03-10_10:03:58.png",
        "channel_desc": "First Nepali Business Channel .",
        "channel_number": "77",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.205/iptvlivestream/netBTV1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1357",
        "channel_name": "Him Shikhar TV",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Him_Shikhar_TV_2021-09-20_06:09:29.png",
        "channel_desc": "nepali local channel",
        "channel_number": "80",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.207/iptvlivestream/netHimShikharTV1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1396",
        "channel_name": "Space 4K",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Space_4K_2023-04-16_09:04:03.png",
        "channel_desc": "Its Nepali Channel for entertainment and News.",
        "channel_number": "81",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.204/iptvlivestream/netSPACE4K.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1361",
        "channel_name": "BM HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/BM_HD_2021-12-17_10:12:50.png",
        "channel_desc": "This is Nepali Movie Channel.",
        "channel_number": "88",
        "category_title": "Nepali",
        "stream_url": "http://202.166.192.206/iptvlivestream/netBM1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "351",
        "channel_name": "Al Jazeera HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Al_Jazeera_HD_2021-05-05_08:05:14.png",
        "channel_desc": "Al Jazeera",
        "channel_number": "200",
        "category_title": "News",
        "stream_url": "http://202.166.192.207/iptvlivestream/netALJAZEERA1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "496",
        "channel_name": "TRT World HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/TRT_WORLD_HD_2021-05-05_08:05:19.png",
        "channel_desc": "TRT World",
        "channel_number": "201",
        "category_title": "News",
        "stream_url": "http://202.166.192.206/iptvlivestream/netTRTWORLDHD1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "425",
        "channel_name": "Sony Entertainment HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Sony_Entertainment_HD_2022-11-06_09:11:07.png",
        "channel_desc": "Most popular Entertainment Sony TV",
        "channel_number": "300",
        "category_title": "Entertainment",
        "stream_url": "http://202.166.192.207/iptvlivedge/netSONYHD2500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "341",
        "channel_name": "Zee TV HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Zee_TV_HD_2025-06-10_12:06:59.png",
        "channel_desc": "India Entertainment Channel.",
        "channel_number": "302",
        "category_title": "Entertainment",
        "stream_url": "http://202.166.192.205/iptvlivedge/netZEETVHD1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "1285",
        "channel_name": "Star Movies HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Star_Movies_HD_2020-12-17_10:12:31.png",
        "channel_desc": "Star Movies HD",
        "channel_number": "406",
        "category_title": "Movies",
        "stream_url": "http://202.166.192.205/iptvlivestream/netStarMoviesHD.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "346",
        "channel_name": "Sony Sports Ten 1 HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Sony_Ten_1_HD_2022-11-06_09:11:20.png",
        "channel_desc": "TEN Sports is Asia's leading sports network",
        "channel_number": "505",
        "category_title": "Sports",
        "stream_url": "http://202.166.192.204/iptvsports/netTEN1HD.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "249",
        "channel_name": "MTV",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/MTV_2020-11-02_12:11:00.png",
        "channel_desc": "MTV India",
        "channel_number": "616",
        "category_title": "Music",
        "stream_url": "http://202.166.192.204/iptvlivestream/netMTV1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "246",
        "channel_name": "Nick Jr",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Nick_Jr_2021-05-05_08:05:12.png",
        "channel_desc": "Nickelodeon",
        "channel_number": "704",
        "category_title": "Kids",
        "stream_url": "http://202.166.192.205/iptvlivestream/netNICKJR1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "580",
        "channel_name": "NAT GEO WILD HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/NAT_GEO_WILD_HD_2021-05-05_08:05:28.png",
        "channel_desc": "Nat Geo Wild is a cable/satellite TV channel focused primarily on wildlife and natural history programming.",
        "channel_number": "801",
        "category_title": "Adventures",
        "stream_url": "http://202.166.192.206/iptvlivestream/netNATGEOWILDHD1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "350",
        "channel_name": "NHK World HD",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/NHK_World_HD_2021-05-05_08:05:07.png",
        "channel_desc": "NHK NEWSLINE delivers the latest news, business, and weather every hour with stories and analysis from Japan, the rest of Asia, and around the world.",
        "channel_number": "851",
        "category_title": "International",
        "stream_url": "http://202.166.192.206/iptvlivestream/netNHK1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    },
    {
        "channel_id": "239",
        "channel_name": "Bhakti Darshan",
        "channel_logo": "https://nettv1.nettv.com.np/channel_logo/Bhakti_Darshan_2021-05-05_08:05:16.png",
        "channel_desc": "Bhakti darshan is a religious channel of nepal.",
        "channel_number": "900",
        "category_title": "Spiritual & Life",
        "stream_url": "http://202.166.192.205/iptvlivestream/netBHAKTIDARSAN1500.stream/playlist.m3u8?wmsAuthSign=c2VydmVyX3RpbWU9NC8zMC8yMDI2IDE6NDA6NTIgQU0maGFzaF92YWx1ZT12aVJGSmtLS0xFZTczWlBCNnFsTHFRPT0mdmFsaWRtaW51dGVzPTE0NDAmaWQ9MjAw"
    }
]"""

# Just a test sample, we actually have the full list stored locally via the original python script or we can fetch it.
# Actually, the user originally had a full channels.json file.
# Wait, I don't need to overwrite channels.json if I can just pull the categories from my Python script using the existing channels.json!

# Let's check what the current channels.json has
try:
    with open('D:/backend-fastapi/zinx/NepalLiveTv/app/src/main/assets/channels.json', 'r') as f:
        current_data = json.load(f)
except Exception as e:
    current_data = []

# Wait, current channels.json does NOT have the 'category' field.
# We need to map it back! I'll extract it from the raw_json in the user's FIRST prompt.

# Let me write a script that fetches the JSON from the user's original context if I paste it entirely into the script.
