from rest_framework import routers
from . import views

router = routers.DefaultRouter()
router.register('Post', views.BlogImages, basename='post')
router.register('Image', views.BlogImages, basename='image')  

urlpatterns = router.urls
