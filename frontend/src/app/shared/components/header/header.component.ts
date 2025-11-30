import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../core/services/auth.service';
import { LanguageService, Language } from '../../../core/services/language.service';
import { Observable } from 'rxjs';
import { User } from '../../../core/models/user.model';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule,
    TranslateModule
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  currentUser$: Observable<User | null>;
  currentLang$: Observable<string>;
  languages: Language[];

  constructor(
    private authService: AuthService,
    private router: Router,
    private languageService: LanguageService
  ) {
    this.currentUser$ = this.authService.currentUser$;
    this.currentLang$ = this.languageService.currentLang$;
    this.languages = this.languageService.languages;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  setLanguage(langCode: string): void {
    this.languageService.setLanguage(langCode);
  }

  getCurrentLanguage(): Language | undefined {
    return this.languageService.getLanguageByCode(this.languageService.getCurrentLanguage());
  }
}
